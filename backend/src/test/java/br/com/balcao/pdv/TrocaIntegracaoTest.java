/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRepository;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.venda.Devolucao;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.TrocaService;
import br.com.balcao.pdv.venda.VendaResponse;
import br.com.balcao.pdv.venda.VendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Troca e devolução parcial com vale-troca ou dinheiro. */
@SpringBootTest
class TrocaIntegracaoTest extends IntegracaoBase {

    @Autowired CaixaService caixaService;
    @Autowired VendaService vendaService;
    @Autowired TrocaService trocaService;
    @Autowired ProdutoService produtoService;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired JdbcTemplate jdbc;

    private Produto camiseta;
    private Long caixaId;

    @BeforeEach
    void preparar() {
        jdbc.execute(LIMPAR_BASE);
        camiseta = produtoService.cadastrar(ProdutoRequest.basico("Camiseta", new BigDecimal("50.00"), "C1", null,
                "UN", "61091000", null, new BigDecimal("10")));
        caixaId = caixaService.abrir(new BigDecimal("200.00")).getId();
    }

    @Test
    void trocaGeraValeComODescontoRateadoEOValePagaOutraVenda() {
        VendaResponse original = vendaPaga(3, new BigDecimal("10")); // 150 − 10% = 135
        Long itemId = original.itens().getFirst().id();

        TrocaService.Resultado troca = trocaService.devolver(original.id(),
                List.of(new TrocaService.ItemDevolvido(itemId, BigDecimal.ONE)), Devolucao.Destino.VALE_TROCA,
                "Tamanho errado", null);

        assertThat(troca.valor()).isEqualByComparingTo("45.00"); // 50 menos o desconto proporcional
        assertThat(troca.codigoVale()).startsWith("VT").hasSize(8);
        assertThat(produtoRepository.findById(camiseta.getId()).orElseThrow().getEstoqueAtual())
                .isEqualByComparingTo("8"); // 10 − 3 + 1

        // Não dá para devolver mais do que sobrou.
        assertThatThrownBy(() -> trocaService.devolver(original.id(),
                List.of(new TrocaService.ItemDevolvido(itemId, new BigDecimal("3"))), Devolucao.Destino.DINHEIRO, null, null))
                .extracting("codigo").isEqualTo("QUANTIDADE_DEVOLVIDA_INVALIDA");

        // O vale paga a nova compra, até o saldo.
        Long nova = vendaService.iniciar().id();
        vendaService.adicionarItem(nova, camiseta.getId(), null, BigDecimal.ONE); // 50
        assertThatThrownBy(() -> vendaService.adicionarPagamento(nova, FormaPagamento.VALE_TROCA, new BigDecimal("50"), troca.codigoVale()))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("VALE_SEM_SALDO");
        vendaService.adicionarPagamento(nova, FormaPagamento.VALE_TROCA, new BigDecimal("45"), troca.codigoVale().toLowerCase());
        vendaService.adicionarPagamento(nova, FormaPagamento.DINHEIRO, new BigDecimal("5"), null);
        vendaService.finalizar(nova);

        assertThat(trocaService.vale(troca.codigoVale()).getSaldo()).isEqualByComparingTo("0");
    }

    @Test
    void devolucaoEmDinheiroSaiDaGavetaEBloqueiaOEstornoTotal() {
        VendaResponse venda = vendaPaga(2, null); // 100 em dinheiro → gaveta 300
        trocaService.devolver(venda.id(),
                List.of(new TrocaService.ItemDevolvido(venda.itens().getFirst().id(), BigDecimal.ONE)),
                Devolucao.Destino.DINHEIRO, null, null);

        assertThat(caixaService.saldoEsperado(caixaService.buscar(caixaId))).isEqualByComparingTo("250.00");
        assertThatThrownBy(() -> vendaService.estornar(venda.id(), "teste"))
                .extracting("codigo").isEqualTo("VENDA_COM_DEVOLUCAO");
    }

    private VendaResponse vendaPaga(int quantidade, BigDecimal descontoPct) {
        Long id = vendaService.iniciar().id();
        VendaResponse v = vendaService.adicionarItem(id, camiseta.getId(), null, BigDecimal.valueOf(quantidade));
        if (descontoPct != null) {
            v = vendaService.aplicarDesconto(id, null, descontoPct, () -> true);
        }
        vendaService.adicionarPagamento(id, FormaPagamento.DINHEIRO, v.total(), null);
        vendaService.finalizar(id);
        return vendaService.detalhar(id);
    }
}
