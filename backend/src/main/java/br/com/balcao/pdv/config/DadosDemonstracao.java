/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.config;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.cliente.Cliente;
import br.com.balcao.pdv.cliente.ClienteService;
import br.com.balcao.pdv.estoque.PoliticaSaldoInsuficiente;
import br.com.balcao.pdv.fiscal.Ambiente;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalDto;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalService;
import br.com.balcao.pdv.fiscal.TipoEmissor;
import br.com.balcao.pdv.loja.LojaService;
import br.com.balcao.pdv.loja.TipoValorBalanca;
import br.com.balcao.pdv.produto.Categoria;
import br.com.balcao.pdv.produto.CategoriaRepository;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRepository;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.usuario.AuthService;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Usuario;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.VendaResponse;
import br.com.balcao.pdv.venda.VendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/**
 * Popula um banco vazio para demonstração: loja com identidade visual, operadores, categorias, produtos,
 * clientes com fiado e 30 dias de vendas geradas pelas regras reais do sistema (caixa, estoque, fiado).
 * Só roda com o perfil {@code demo}. PINs de acesso: ver o README. GTINs com prefixo 200 (uso interno GS1).
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DadosDemonstracao implements ApplicationRunner {

    private static final String LOGO_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="16" fill="#D9482B"/>\
            <path d="M14 24h36l-4 22a4 4 0 0 1-4 3H22a4 4 0 0 1-4-3z" fill="#FFF7EE"/>\
            <path d="M24 24l6-10M40 24l-6-10" stroke="#FFF7EE" stroke-width="4" stroke-linecap="round"/>\
            <circle cx="26" cy="35" r="2.6" fill="#D9482B"/><circle cx="38" cy="35" r="2.6" fill="#D9482B"/></svg>""";

    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;
    private final CategoriaRepository categoriaRepository;
    private final ConfiguracaoFiscalService configuracaoFiscalService;
    private final LojaService lojaService;
    private final AuthService authService;
    private final ClienteService clienteService;
    private final CaixaService caixaService;
    private final VendaService vendaService;
    private final RelogioAjustavel relogio;
    private final br.com.balcao.pdv.caixa.TerminalRepository terminalRepository;
    private final br.com.balcao.pdv.estoque.EstoqueService estoqueService;

    private final Random aleatorio = new Random(42);

    @Override
    public void run(ApplicationArguments args) {
        if (produtoRepository.count() > 0) {
            return;
        }
        long inicio = System.currentTimeMillis();
        log.info("Perfil demo: montando loja, produtos, clientes e 30 dias de vendas…");
        configurarLoja();
        List<Usuario> equipe = equipe();
        List<Produto> produtos = produtos();
        List<Cliente> clientes = clientes();
        try {
            historico(equipe, produtos, clientes);
        } finally {
            relogio.voltarAoPresente();
        }
        // Caixa de hoje aberto desde as 7h, com as vendas até agora, pronto para continuar vendendo.
        try {
            hoje(equipe, produtos, clientes);
        } finally {
            relogio.voltarAoPresente();
        }
        log.info("Perfil demo pronto em {} s", (System.currentTimeMillis() - inicio) / 1000);
    }

    private void configurarLoja() {
        String logo = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(LOGO_SVG.getBytes(StandardCharsets.UTF_8));
        lojaService.atualizar(new LojaService.LojaRequest("Mercadinho Exemplo", "Do bairro, pro bairro", logo,
                "#D9482B", "Obrigado pela preferência! Volte sempre.", PoliticaSaldoInsuficiente.PERMITIR_E_AVISAR,
                new BigDecimal("600.00"), new BigDecimal("5"), "2", 4, TipoValorBalanca.PRECO,
                "pix@mercadinhoexemplo.com.br", "Mercadinho Exemplo", "Sao Paulo", new BigDecimal("31.45"), 10));
        // Um segundo ponto de venda (balcão externo) para mostrar vários caixas ao mesmo tempo.
        terminalRepository.save(new br.com.balcao.pdv.caixa.Terminal("Caixa 02"));
        configuracaoFiscalService.atualizar(new ConfiguracaoFiscalDto.Request(
                "11222333000181", "111111111111", "MERCADINHO EXEMPLO LTDA", "Mercadinho Exemplo", 1,
                "Rua das Flores", "100", "Centro", "3550308", "São Paulo", "SP", "01001000", "1130000000",
                Ambiente.HOMOLOGACAO, 1, 1, "1", "DEMO0000CSC0000HOMOLOGACAO000001",
                "https://www.homologacao.nfce.fazenda.sp.gov.br/qrcode",
                "https://www.homologacao.nfce.fazenda.sp.gov.br/consulta",
                true, 30, TipoEmissor.SIMULADO));
    }

    private List<Usuario> equipe() {
        return List.of(
                authService.criar("Dono", Papel.ADMIN, "1234"),
                authService.criar("Ana", Papel.GERENTE, "1234"),
                authService.criar("Bruno", Papel.OPERADOR, "1234"),
                authService.criar("Carla", Papel.OPERADOR, "1234"));
    }

    private List<Produto> produtos() {
        Categoria bebidas = categoriaRepository.save(new Categoria("Bebidas", "#2F6FDB"));
        Categoria mercearia = categoriaRepository.save(new Categoria("Mercearia", "#C98A1B"));
        Categoria laticinios = categoriaRepository.save(new Categoria("Laticínios", "#6BA8C9"));
        Categoria doces = categoriaRepository.save(new Categoria("Doces e snacks", "#C2417A"));
        Categoria limpeza = categoriaRepository.save(new Categoria("Limpeza", "#3E9C74"));
        Categoria padaria = categoriaRepository.save(new Categoria("Padaria", "#B5652B"));
        Categoria hortifruti = categoriaRepository.save(new Categoria("Hortifrúti", "#5E9E2F"));

        List<Produto> p = new ArrayList<>();
        p.add(produto("Refrigerante Cola 2L", "10.99", "6.80", "2000000000015", "001", "UN", "22021000", bebidas, 180, 24, false));
        p.add(produto("Água Mineral 500ml", "2.50", "0.95", "2000000000022", "002", "UN", "22011000", bebidas, 400, 48, true));
        p.add(produto("Café Torrado e Moído 500g", "18.90", "12.40", "2000000000039", "003", "UN", "09012100", mercearia, 90, 12, false));
        p.add(produto("Arroz Branco Tipo 1 5kg", "27.90", "21.30", "2000000000046", "004", "UN", "10063021", mercearia, 70, 10, false));
        p.add(produto("Feijão Carioca 1kg", "8.49", "5.90", "2000000000053", "005", "UN", "07133319", mercearia, 110, 15, false));
        p.add(produto("Açúcar Refinado 1kg", "4.99", "3.40", "2000000000060", "006", "UN", "17019900", mercearia, 90, 15, false));
        p.add(produto("Leite Integral 1L", "5.29", "3.85", "2000000000077", "007", "UN", "04012010", laticinios, 220, 36, false));
        p.add(produto("Biscoito Recheado 130g", "3.49", "1.90", "2000000000084", "008", "UN", "19053100", doces, 150, 20, false));
        p.add(produto("Chocolate ao Leite 90g", "6.99", "3.95", "2000000000091", "009", "UN", "18063210", doces, 60, 12, false));
        p.add(produto("Óleo de Soja 900ml", "7.89", "5.60", "2000000000107", "010", "UN", "15079011", mercearia, 80, 12, false));
        p.add(produto("Macarrão Espaguete 500g", "4.59", "2.80", "2000000000114", "011", "UN", "19021900", mercearia, 120, 15, false));
        p.add(produto("Detergente Neutro 500ml", "2.79", "1.55", "2000000000121", "012", "UN", "34022000", limpeza, 100, 15, false));
        p.add(produto("Papel Higiênico 12 rolos", "21.90", "15.10", "2000000000138", "013", "UN", "48181000", limpeza, 45, 8, false));
        p.add(produto("Sabão em Pó 1kg", "14.50", "9.70", "2000000000145", "014", "UN", "34022000", limpeza, 40, 8, false));
        p.add(produto("Pão Francês (kg)", "16.90", "7.20", null, "015", "KG", "19059090", padaria, 120, null, true));
        p.add(produto("Banana Prata (kg)", "6.49", "3.10", null, "016", "KG", "08039000", hortifruti, 150, null, true));
        p.add(produto("Iogurte Morango 170g", "3.29", "1.95", "2000000000152", "017", "UN", "04031000", laticinios, 90, 12, false));
        p.add(produto("Cerveja Lata 350ml", "4.29", "2.70", "2000000000169", "018", "UN", "22030000", bebidas, 300, 48, false));
        p.add(produto("Queijo Muçarela (kg)", "49.90", "32.00", null, "019", "KG", "04061010", laticinios, 25, 3, false));
        p.add(produto("Cafezinho", "3.00", "0.60", null, "020", "UN", "21011110", padaria, 999, null, true));
        p.add(produto("Sacola Retornável", "4.90", "2.10", null, "021", "UN", "42022220", mercearia, 80, 10, true));

        // Oferta da semana: vale na venda automaticamente.
        Produto refri = p.get(0);
        produtoService.atualizar(refri.getId(), new ProdutoRequest(refri.getNome(), refri.getPreco(), "001",
                refri.getGtin(), "UN", refri.getNcm(), "5102", 0, "102", new BigDecimal("24"), null,
                refri.getCategoria().getId(), refri.getPrecoCusto(), new BigDecimal("9.49"),
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), false, null));
        return p;
    }

    private Produto produto(String nome, String preco, String custo, String gtin, String codigo, String unidade,
                            String ncm, Categoria categoria, int estoque, Integer minimo, boolean atalho) {
        return produtoService.cadastrar(new ProdutoRequest(nome, new BigDecimal(preco), codigo, gtin, unidade, ncm,
                "5102", 0, "102", minimo != null ? BigDecimal.valueOf(minimo) : null, BigDecimal.valueOf(estoque * 3L),
                categoria.getId(), new BigDecimal(custo), null, null, null, atalho, null));
    }

    private List<Cliente> clientes() {
        return List.of(
                cliente("Dona Maria das Graças", "52998224725", "11987654321", "300"),
                cliente("Seu João Batista", null, "11912345678", "200"),
                cliente("Oficina do Zé", "11444777000161", "1133334444", "800"),
                cliente("Juliana Costa", null, "11955554444", "0"));
    }

    private Cliente cliente(String nome, String doc, String tel, String limite) {
        return clienteService.salvar(null, new ClienteService.ClienteRequest(nome, doc, tel, null,
                new BigDecimal(limite), null, true));
    }

    /** 30 dias de movimento: um caixa por dia, vendas das 7h às 21h, pico no almoço e no fim da tarde. */
    private void historico(List<Usuario> equipe, List<Produto> produtos, List<Cliente> clientes) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoje = LocalDate.now();
        int[] pesoHora = {0, 0, 0, 0, 0, 0, 0, 3, 5, 4, 4, 6, 8, 6, 4, 4, 5, 8, 9, 7, 4, 2, 0, 0};
        for (int d = 30; d >= 1; d--) {
            LocalDate dia = hoje.minusDays(d);
            boolean domingo = dia.getDayOfWeek().getValue() == 7;
            Usuario operador = equipe.get(2 + aleatorio.nextInt(2));
            relogio.irPara(dia.atTime(7, 2).atZone(zona).toInstant());
            var caixa = caixaService.abrir(new BigDecimal("150.00"), operador.getId());

            int vendas = (domingo ? 10 : 18) + aleatorio.nextInt(12) + (30 - d) / 3;
            List<Integer> horas = new ArrayList<>();
            for (int i = 0; i < vendas; i++) {
                horas.add(sortearHora(pesoHora));
            }
            horas.sort(Integer::compare);
            for (int hora : horas) {
                relogio.irPara(dia.atTime(hora, aleatorio.nextInt(60), aleatorio.nextInt(60)).atZone(zona).toInstant());
                venda(operador, produtos, clientes);
            }
            relogio.irPara(dia.atTime(21, 30).atZone(zona).toInstant());
            BigDecimal esperado = caixaService.saldoEsperado(caixaService.buscar(caixa.getId()));
            if (esperado.compareTo(new BigDecimal("700")) > 0) {
                caixaService.sangria(caixa.getId(), esperado.subtract(new BigDecimal("150")).setScale(0, RoundingMode.DOWN),
                        "Cofre", equipe.get(1).getId());
                esperado = caixaService.saldoEsperado(caixaService.buscar(caixa.getId()));
            }
            reporEstoque(produtos);
            // A maioria dos dias confere; alguns têm sobra ou falta de centavos/poucos reais.
            int sorteio = aleatorio.nextInt(10);
            BigDecimal diferenca = sorteio < 7 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(aleatorio.nextInt(900) - 450, 2);
            caixaService.fechar(caixa.getId(), esperado.add(diferenca).max(BigDecimal.ZERO), operador.getId());
        }
    }

    private void hoje(List<Usuario> equipe, List<Produto> produtos, List<Cliente> clientes) {
        ZoneId zona = ZoneId.systemDefault();
        var agora = java.time.LocalDateTime.now();
        Usuario operador = equipe.get(2);
        relogio.irPara(agora.toLocalDate().atTime(7, 0).atZone(zona).toInstant());
        caixaService.abrir(new BigDecimal("150.00"), operador.getId());
        int[] pesoHora = {0, 0, 0, 0, 0, 0, 0, 3, 5, 4, 4, 6, 8, 6, 4, 4, 5, 8, 9, 7, 4, 2, 0, 0};
        List<java.time.LocalDateTime> momentos = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            var m = agora.toLocalDate().atTime(sortearHora(pesoHora), aleatorio.nextInt(60), aleatorio.nextInt(60));
            if (m.isBefore(agora.minusMinutes(5))) {
                momentos.add(m);
            }
        }
        momentos.sort(null);
        for (var m : momentos) {
            relogio.irPara(m.atZone(zona).toInstant());
            venda(operador, produtos, clientes);
        }
    }

    /** Fim do dia: repõe o que baixou demais, como o dono faria (entrada de mercadoria). */
    private void reporEstoque(List<Produto> produtos) {
        for (Produto p : produtos) {
            BigDecimal atual = produtoRepository.findById(p.getId()).orElseThrow().getEstoqueAtual();
            BigDecimal alvo = BigDecimal.valueOf("KG".equals(p.getUnidade()) ? 40 : 60);
            if (atual.compareTo(BigDecimal.valueOf(15)) < 0 && !"020".equals(p.getCodigoInterno())) {
                estoqueService.entrada(p.getId(), alvo.subtract(atual),
                        "Reposição do fornecedor");
            }
        }
    }

    private void venda(Usuario operador, List<Produto> produtos, List<Cliente> clientes) {
        VendaResponse v = vendaService.iniciar(operador.getId());
        int itens = 1 + aleatorio.nextInt(aleatorio.nextInt(10) < 7 ? 4 : 8);
        for (int i = 0; i < itens; i++) {
            Produto p = produtos.get(sortearProduto(produtos.size()));
            BigDecimal qtd = "KG".equals(p.getUnidade())
                    ? BigDecimal.valueOf(150 + aleatorio.nextInt(1200), 3)
                    : BigDecimal.valueOf(1 + (aleatorio.nextInt(10) < 8 ? 0 : aleatorio.nextInt(4)));
            v = vendaService.adicionarItem(v.id(), p.getId(), null, qtd);
        }
        int acaso = aleatorio.nextInt(100);
        if (acaso < 3) {
            vendaService.cancelar(v.id(), "Cliente desistiu");
            return;
        }
        if (acaso < 9 && v.total().compareTo(new BigDecimal("30")) > 0) {
            v = vendaService.aplicarDesconto(v.id(), null, BigDecimal.valueOf(2 + aleatorio.nextInt(4)), () -> true);
        }
        FormaPagamento forma = sortearForma();
        if (forma == FormaPagamento.CREDIARIO) {
            Cliente c = clientes.get(aleatorio.nextInt(3));
            v = vendaService.vincularCliente(v.id(), c.getId());
            if (c.getLimiteCredito().subtract(clienteService.buscar(c.getId()).getSaldoDevedor()).compareTo(v.total()) < 0) {
                forma = FormaPagamento.PIX;
            }
        }
        if (forma == FormaPagamento.DINHEIRO) {
            vendaService.adicionarPagamento(v.id(), forma, notaParaPagar(v.total()), null);
        } else if (acaso > 90 && forma != FormaPagamento.CREDIARIO && v.total().compareTo(new BigDecimal("20")) > 0) {
            // Pagamento dividido: parte em PIX/cartão, resto em dinheiro.
            BigDecimal parte = v.total().divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN);
            v = vendaService.adicionarPagamento(v.id(), forma, parte, null);
            vendaService.adicionarPagamento(v.id(), FormaPagamento.DINHEIRO, notaParaPagar(v.restante()), null);
        } else {
            vendaService.adicionarPagamento(v.id(), forma, v.total(), null);
        }
        vendaService.finalizar(v.id());
    }

    private int sortearHora(int[] pesos) {
        int total = 0;
        for (int p : pesos) {
            total += p;
        }
        int r = aleatorio.nextInt(total);
        for (int h = 0; h < pesos.length; h++) {
            r -= pesos[h];
            if (r < 0) {
                return h;
            }
        }
        return 12;
    }

    /** Os primeiros da lista vendem mais (distribuição tipo curva ABC). */
    private int sortearProduto(int n) {
        double x = aleatorio.nextDouble();
        return Math.min(n - 1, (int) (n * x * x));
    }

    private FormaPagamento sortearForma() {
        int r = aleatorio.nextInt(100);
        if (r < 30) return FormaPagamento.DINHEIRO;
        if (r < 65) return FormaPagamento.PIX;
        if (r < 80) return FormaPagamento.CARTAO_DEBITO;
        if (r < 90) return FormaPagamento.CARTAO_CREDITO;
        if (r < 95) return FormaPagamento.VALE_ALIMENTACAO;
        return FormaPagamento.CREDIARIO;
    }

    /** O cliente paga com a nota "redonda" acima do valor, como no balcão. */
    private BigDecimal notaParaPagar(BigDecimal valor) {
        for (int nota : new int[]{5, 10, 20, 50, 100, 200}) {
            if (BigDecimal.valueOf(nota).compareTo(valor) >= 0 && aleatorio.nextInt(3) > 0) {
                return BigDecimal.valueOf(nota);
            }
        }
        return valor.setScale(0, RoundingMode.UP);
    }
}
