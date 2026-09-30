package br.com.balcao.pdv;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.caixa.ExtratoCaixa;
import br.com.balcao.pdv.caixa.SituacaoConferencia;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.MovimentacaoEstoqueRepository;
import br.com.balcao.pdv.fiscal.Ambiente;
import br.com.balcao.pdv.fiscal.ChaveAcesso;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalDto;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalService;
import br.com.balcao.pdv.fiscal.NfceService;
import br.com.balcao.pdv.fiscal.NotaFiscalRepository;
import br.com.balcao.pdv.fiscal.StatusNota;
import br.com.balcao.pdv.fiscal.TipoEmissor;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRepository;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.VendaResponse;
import br.com.balcao.pdv.venda.VendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fluxos ponta a ponta contra um PostgreSQL real (Testcontainers). */
@SpringBootTest
@Testcontainers
class FluxoCompletoIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired CaixaService caixaService;
    @Autowired VendaService vendaService;
    @Autowired ProdutoService produtoService;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Autowired NfceService nfceService;
    @Autowired NotaFiscalRepository notaRepository;
    @Autowired ConfiguracaoFiscalService configuracaoFiscalService;
    @Autowired JdbcTemplate jdbc;

    private Produto refri;
    private Produto cafe;

    @BeforeEach
    void limparBase() {
        jdbc.execute("TRUNCATE nota_fiscal, movimentacao_caixa, movimentacao_estoque, pagamento, item_venda, venda, "
                + "caixa, produto RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE configuracao_fiscal SET proximo_numero = 1, emissao_automatica = true");
        refri = produtoService.cadastrar(produto("Refrigerante 2L", "9.50", "2000000000015", 20));
        cafe = produtoService.cadastrar(produto("Café 500g", "18.90", "2000000000039", 10));
        configurarEmitente();
    }

    @Test
    void vendaEmDinheiroEntraLiquidaNoCaixaEBaixaEstoque() {
        var caixa = caixaService.abrir(new BigDecimal("100.00"));
        Long vendaId = vendaService.iniciar().id();
        vendaService.adicionarItem(vendaId, refri.getId(), null, new BigDecimal("5")); // 47,50
        vendaService.adicionarPagamento(vendaId, FormaPagamento.DINHEIRO, new BigDecimal("50.00"), null);

        vendaService.finalizar(vendaId);

        assertThat(caixaService.saldoEsperado(caixaService.buscar(caixa.getId()))).isEqualByComparingTo("147.50");
        assertThat(produtoRepository.findById(refri.getId()).orElseThrow().getEstoqueAtual())
                .isEqualByComparingTo("15");
        // RN-EST-02: saldo sempre igual à soma do histórico.
        assertThat(movimentacaoEstoqueRepository.somaPorProduto(refri.getId())).isEqualByComparingTo("15");
    }

    @Test
    void naoFechaCaixaComVendaAbertaEConfereFalta() {
        var caixa = caixaService.abrir(new BigDecimal("500.00"));
        Long vendaId = vendaService.iniciar().id();

        assertThatThrownBy(() -> caixaService.fechar(caixa.getId(), new BigDecimal("500")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("CAIXA_COM_VENDA_ABERTA");

        vendaService.cancelar(vendaId, "Cliente desistiu");
        ExtratoCaixa extrato = caixaService.fechar(caixa.getId(), new BigDecimal("480.00"));

        assertThat(extrato.diferenca()).isEqualByComparingTo("-20.00");
        assertThat(extrato.situacaoConferencia()).isEqualTo(SituacaoConferencia.FALTA);
        assertThat(extrato.vendasCanceladas()).isEqualTo(1);
    }

    @Test
    void naoPermiteDoisCaixasAbertos() {
        caixaService.abrir(BigDecimal.TEN);

        assertThatThrownBy(() -> caixaService.abrir(BigDecimal.TEN))
                .extracting("codigo").isEqualTo("CAIXA_JA_ABERTO");
    }

    @Test
    void emiteNfceComNumeracaoSequencialEBloqueiaEstornoAteCancelar() {
        caixaService.abrir(new BigDecimal("50.00"));
        Long v1 = vendaPaga(refri, FormaPagamento.PIX);
        vendaService.finalizar(v1);
        assertThat(nfceService.emitirSeAutomatico(v1)).isEmpty();

        Long v2 = vendaPaga(cafe, FormaPagamento.CARTAO_DEBITO);
        vendaService.finalizar(v2);
        assertThat(nfceService.emitirSeAutomatico(v2)).isEmpty();

        var nota1 = notaRepository.findFirstByVendaIdOrderByIdDesc(v1).orElseThrow();
        var nota2 = notaRepository.findFirstByVendaIdOrderByIdDesc(v2).orElseThrow();
        assertThat(nota1.getStatus()).isEqualTo(StatusNota.AUTORIZADA);
        assertThat(nota1.getNumero()).isEqualTo(1);
        assertThat(nota2.getNumero()).isEqualTo(2);
        assertThat(ChaveAcesso.valida(nota1.getChaveAcesso())).isTrue();
        assertThat(nota1.getXml()).contains("<tPag>17</tPag>").contains("<NCM>22021000</NCM>");

        assertThatThrownBy(() -> vendaService.estornar(v1, "teste"))
                .extracting("codigo").isEqualTo("NOTA_AUTORIZADA");
        nfceService.cancelar(nota1.getId(), "Cliente desistiu da compra no caixa");
        VendaResponse estornada = vendaService.estornar(v1, "Cliente desistiu");

        assertThat(estornada.status().name()).isEqualTo("ESTORNADA");
        assertThat(produtoRepository.findById(refri.getId()).orElseThrow().getEstoqueAtual())
                .isEqualByComparingTo("20");
    }

    @Test
    void falhaNaNotaNaoDesfazAVenda() {
        caixaService.abrir(BigDecimal.TEN);
        jdbc.update("UPDATE produto SET ncm = NULL WHERE id = ?", refri.getId());
        Long vendaId = vendaPaga(refri, FormaPagamento.DINHEIRO);

        vendaService.finalizar(vendaId);

        assertThat(nfceService.emitirSeAutomatico(vendaId)).hasValueSatisfying(aviso ->
                assertThat(aviso).contains("NCM"));
        assertThat(vendaService.detalhar(vendaId).status().name()).isEqualTo("FINALIZADA");
        // Nenhum número foi consumido.
        assertThat(configuracaoFiscalService.obter().getProximoNumero()).isEqualTo(1);
    }

    private Long vendaPaga(Produto produto, FormaPagamento forma) {
        Long id = vendaService.iniciar().id();
        VendaResponse v = vendaService.adicionarItem(id, produto.getId(), null, BigDecimal.ONE);
        vendaService.adicionarPagamento(id, forma, v.total(), null);
        return id;
    }

    private void configurarEmitente() {
        configuracaoFiscalService.atualizar(new ConfiguracaoFiscalDto.Request(
                "11222333000181", "111111111111", "EMPRESA TESTE LTDA", "Teste", 1, "Rua A", "1", "Centro",
                "3550308", "São Paulo", "SP", "01001000", null, Ambiente.HOMOLOGACAO, 1, null, "1", "CSC-TESTE",
                "https://sefaz.exemplo/qrcode", "https://sefaz.exemplo/consulta", true, 30, TipoEmissor.SIMULADO));
    }

    private static ProdutoRequest produto(String nome, String preco, String gtin, int estoque) {
        return new ProdutoRequest(nome, new BigDecimal(preco), null, gtin, "UN", "22021000", "5102", 0, "102",
                null, BigDecimal.valueOf(estoque));
    }
}
