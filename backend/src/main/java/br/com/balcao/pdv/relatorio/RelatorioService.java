package br.com.balcao.pdv.relatorio;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.cliente.ClienteService;
import br.com.balcao.pdv.comum.Dinheiro;
import br.com.balcao.pdv.loja.LojaService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas agregadas do painel e dos relatórios (SQL direto, só leitura). */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final String FINALIZADAS_NO_PERIODO =
            "v.status = 'FINALIZADA' and v.data_finalizacao >= :inicio and v.data_finalizacao < :fim";

    private final NamedParameterJdbcTemplate jdbc;
    private final CaixaService caixaService;
    private final ClienteService clienteService;
    private final LojaService lojaService;

    public record Resumo(long vendas, BigDecimal faturamento, BigDecimal ticketMedio, BigDecimal itens,
                         BigDecimal descontos, BigDecimal lucroBruto, BigDecimal margem,
                         BigDecimal coberturaCusto, long canceladas) {
    }

    public record Serie(String rotulo, BigDecimal valor, long quantidade) {
    }

    public record ProdutoRanking(Long produtoId, String nome, BigDecimal quantidade, BigDecimal valor,
                                 BigDecimal lucro) {
    }

    public record Alertas(long estoqueBaixo, long estoqueZerado, long notasComProblema, BigDecimal fiadoAReceber,
                          BigDecimal gaveta, BigDecimal limiteGaveta, long vendasEmEspera) {
    }

    public record Painel(LocalDate dia, Resumo hoje, Resumo ontem, List<Serie> porHora, List<Serie> porForma,
                         List<Serie> porCategoria, List<Serie> porOperador, List<Serie> ultimos30Dias,
                         List<ProdutoRanking> maisVendidos, Alertas alertas) {
    }

    public record ItemAbc(Long produtoId, String nome, BigDecimal quantidade, BigDecimal valor, BigDecimal percentual,
                          BigDecimal acumulado, String classe) {
    }

    public record CurvaAbc(LocalDate inicio, LocalDate fim, BigDecimal total, List<ItemAbc> itens,
                           Map<String, Long> produtosPorClasse) {
    }

    @Transactional(readOnly = true)
    public Painel painel(LocalDate dia) {
        MapSqlParameterSource hoje = periodo(dia, dia.plusDays(1));
        return new Painel(
                dia,
                resumo(hoje),
                resumo(periodo(dia.minusDays(1), dia)),
                porHora(hoje),
                porForma(hoje),
                series(hoje, """
                        select coalesce(c.nome, 'Sem categoria') rotulo, sum(i.subtotal) valor, count(distinct v.id) qtd
                        from venda v join item_venda i on i.venda_id = v.id join produto p on p.id = i.produto_id
                        left join categoria c on c.id = p.categoria_id
                        where %s group by 1 order by 2 desc
                        """),
                series(hoje, """
                        select coalesce(u.nome, 'Sem operador') rotulo, sum(v.total) valor, count(*) qtd
                        from venda v left join usuario u on u.id = v.operador_id
                        where %s group by 1 order by 2 desc
                        """),
                ultimosDias(dia, 30),
                ranking(hoje, 10),
                alertas());
    }

    @Transactional(readOnly = true)
    public CurvaAbc curvaAbc(LocalDate inicio, LocalDate fim) {
        List<ProdutoRanking> todos = ranking(periodo(inicio, fim.plusDays(1)), 100_000);
        BigDecimal total = todos.stream().map(ProdutoRanking::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ItemAbc> itens = new ArrayList<>();
        Map<String, Long> porClasse = new LinkedHashMap<>(Map.of("A", 0L, "B", 0L, "C", 0L));
        BigDecimal acumulado = BigDecimal.ZERO;
        for (ProdutoRanking p : todos) {
            BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO
                    : p.valor().multiply(CEM).divide(total, 2, RoundingMode.HALF_EVEN);
            // A classe é decidida pelo acumulado ANTES do item: o item que cruza 80% ainda é A.
            String classe = acumulado.compareTo(BigDecimal.valueOf(80)) < 0 ? "A"
                    : acumulado.compareTo(BigDecimal.valueOf(95)) < 0 ? "B" : "C";
            acumulado = acumulado.add(pct);
            porClasse.merge(classe, 1L, Long::sum);
            itens.add(new ItemAbc(p.produtoId(), p.nome(), p.quantidade(), p.valor(), pct,
                    acumulado.min(CEM), classe));
        }
        return new CurvaAbc(inicio, fim, Dinheiro.valor(total), itens, porClasse);
    }

    // --------------------------------------------------------------------------------------------

    private Resumo resumo(MapSqlParameterSource p) {
        Map<String, Object> v = jdbc.queryForMap("""
                select count(*) vendas, coalesce(sum(v.total), 0) faturamento, coalesce(sum(v.desconto), 0) descontos,
                       coalesce(sum(v.subtotal), 0) subtotal
                from venda v where %s
                """.formatted(FINALIZADAS_NO_PERIODO), p);
        Map<String, Object> i = jdbc.queryForMap("""
                select coalesce(sum(i.quantidade), 0) itens,
                       coalesce(sum(case when i.custo_unitario is not null then i.subtotal end), 0) receita_com_custo,
                       coalesce(sum(i.custo_unitario * i.quantidade), 0) custo
                from venda v join item_venda i on i.venda_id = v.id where %s
                """.formatted(FINALIZADAS_NO_PERIODO), p);
        long canceladas = jdbc.queryForObject("""
                select count(*) from venda v
                where v.status in ('CANCELADA', 'ESTORNADA')
                  and v.data_cancelamento >= :inicio and v.data_cancelamento < :fim
                """, p, Long.class);

        long vendas = ((Number) v.get("vendas")).longValue();
        BigDecimal faturamento = dec(v.get("faturamento"));
        BigDecimal subtotal = dec(v.get("subtotal"));
        BigDecimal descontos = dec(v.get("descontos"));
        BigDecimal receitaComCusto = dec(i.get("receita_com_custo"));
        BigDecimal custo = dec(i.get("custo"));

        // Lucro só sobre os itens com custo cadastrado, descontando a parte proporcional dos descontos.
        BigDecimal descontoProporcional = subtotal.signum() == 0 ? BigDecimal.ZERO
                : descontos.multiply(receitaComCusto).divide(subtotal, 2, RoundingMode.HALF_EVEN);
        BigDecimal lucro = receitaComCusto.subtract(descontoProporcional).subtract(custo);
        BigDecimal receitaLiquidaComCusto = receitaComCusto.subtract(descontoProporcional);
        return new Resumo(
                vendas,
                Dinheiro.valor(faturamento),
                vendas == 0 ? Dinheiro.ZERO : faturamento.divide(BigDecimal.valueOf(vendas), 2, RoundingMode.HALF_EVEN),
                dec(i.get("itens")),
                Dinheiro.valor(descontos),
                Dinheiro.valor(lucro),
                receitaLiquidaComCusto.signum() == 0 ? null
                        : lucro.multiply(CEM).divide(receitaLiquidaComCusto, 1, RoundingMode.HALF_EVEN),
                subtotal.signum() == 0 ? null : receitaComCusto.multiply(CEM).divide(subtotal, 0, RoundingMode.HALF_EVEN),
                canceladas);
    }

    private List<Serie> porHora(MapSqlParameterSource p) {
        Map<Integer, Serie> horas = new LinkedHashMap<>();
        jdbc.query("""
                select extract(hour from v.data_finalizacao at time zone :zona)::int hora, sum(v.total) valor, count(*) qtd
                from venda v where %s group by 1
                """.formatted(FINALIZADAS_NO_PERIODO), p, rs -> {
            int h = rs.getInt("hora");
            horas.put(h, new Serie(String.format("%02dh", h), Dinheiro.valor(rs.getBigDecimal("valor")),
                    rs.getLong("qtd")));
        });
        List<Serie> dia = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            dia.add(horas.getOrDefault(h, new Serie(String.format("%02dh", h), Dinheiro.ZERO, 0)));
        }
        return dia;
    }

    /** Por forma de pagamento, com o dinheiro líquido do troco. */
    private List<Serie> porForma(MapSqlParameterSource p) {
        return jdbc.query("""
                select pg.forma rotulo,
                       sum(pg.valor) - case when pg.forma = 'DINHEIRO'
                           then (select coalesce(sum(v.troco), 0) from venda v where %1$s) else 0 end valor,
                       count(distinct pg.venda_id) qtd
                from pagamento pg join venda v on v.id = pg.venda_id
                where %1$s group by pg.forma order by 2 desc
                """.formatted(FINALIZADAS_NO_PERIODO), p,
                (rs, n) -> new Serie(rs.getString("rotulo"), Dinheiro.valor(rs.getBigDecimal("valor")), rs.getLong("qtd")));
    }

    private List<Serie> series(MapSqlParameterSource p, String sql) {
        return jdbc.query(sql.formatted(FINALIZADAS_NO_PERIODO), p,
                (rs, n) -> new Serie(rs.getString("rotulo"), Dinheiro.valor(rs.getBigDecimal("valor")), rs.getLong("qtd")));
    }

    private List<Serie> ultimosDias(LocalDate dia, int dias) {
        MapSqlParameterSource p = periodo(dia.minusDays(dias - 1L), dia.plusDays(1));
        Map<LocalDate, Serie> porDia = new LinkedHashMap<>();
        jdbc.query("""
                select (v.data_finalizacao at time zone :zona)::date dia, sum(v.total) valor, count(*) qtd
                from venda v where %s group by 1
                """.formatted(FINALIZADAS_NO_PERIODO), p, rs -> {
            LocalDate d = rs.getObject("dia", LocalDate.class);
            porDia.put(d, new Serie(d.toString(), Dinheiro.valor(rs.getBigDecimal("valor")), rs.getLong("qtd")));
        });
        List<Serie> serie = new ArrayList<>();
        for (LocalDate d = dia.minusDays(dias - 1L); !d.isAfter(dia); d = d.plusDays(1)) {
            serie.add(porDia.getOrDefault(d, new Serie(d.toString(), Dinheiro.ZERO, 0)));
        }
        return serie;
    }

    private List<ProdutoRanking> ranking(MapSqlParameterSource p, int limite) {
        p.addValue("limite", limite);
        return jdbc.query("""
                select i.produto_id, max(i.descricao) nome, sum(i.quantidade) qtd, sum(i.subtotal) valor,
                       sum(case when i.custo_unitario is not null then i.subtotal - i.custo_unitario * i.quantidade end) lucro
                from venda v join item_venda i on i.venda_id = v.id
                where %s group by i.produto_id order by valor desc limit :limite
                """.formatted(FINALIZADAS_NO_PERIODO), p,
                (rs, n) -> new ProdutoRanking(rs.getLong("produto_id"), rs.getString("nome"), rs.getBigDecimal("qtd"),
                        Dinheiro.valor(rs.getBigDecimal("valor")),
                        rs.getBigDecimal("lucro") != null ? Dinheiro.valor(rs.getBigDecimal("lucro")) : null));
    }

    private Alertas alertas() {
        MapSqlParameterSource p = new MapSqlParameterSource();
        Map<String, Object> e = jdbc.queryForMap("""
                select count(*) filter (where estoque_minimo is not null and estoque_atual <= estoque_minimo) baixo,
                       count(*) filter (where estoque_atual <= 0) zerado
                from produto where ativo
                """, p);
        long notas = jdbc.queryForObject("""
                select count(*) from nota_fiscal where status in ('REJEITADA', 'PENDENTE')
                """, p, Long.class);
        var caixa = caixaService.aberto();
        BigDecimal gaveta = caixa.map(caixaService::saldoEsperado).orElse(null);
        long espera = caixa.map(c -> jdbc.queryForObject(
                "select count(*) from venda where caixa_id = :c and status = 'ABERTA' and em_espera",
                new MapSqlParameterSource("c", c.getId()), Long.class)).orElse(0L);
        return new Alertas(((Number) e.get("baixo")).longValue(), ((Number) e.get("zerado")).longValue(), notas,
                clienteService.totalAReceber(), gaveta, lojaService.obter().getLimiteGaveta(), espera);
    }

    private static MapSqlParameterSource periodo(LocalDate inicio, LocalDate fimExclusivo) {
        ZoneId zona = ZoneId.systemDefault();
        return new MapSqlParameterSource()
                .addValue("inicio", Timestamp.from(inicio.atStartOfDay(zona).toInstant()))
                .addValue("fim", Timestamp.from(fimExclusivo.atStartOfDay(zona).toInstant()))
                .addValue("zona", zona.getId());
    }

    private static BigDecimal dec(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
