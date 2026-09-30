package br.com.balcao.pdv;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.cliente.Cliente;
import br.com.balcao.pdv.cliente.ClienteService;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.EntradaNfeService;
import br.com.balcao.pdv.estoque.PoliticaSaldoInsuficiente;
import br.com.balcao.pdv.fiscal.Ambiente;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalDto;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalService;
import br.com.balcao.pdv.fiscal.NfceService;
import br.com.balcao.pdv.fiscal.NotaFiscalRepository;
import br.com.balcao.pdv.fiscal.TipoEmissor;
import br.com.balcao.pdv.loja.LojaService;
import br.com.balcao.pdv.loja.TipoValorBalanca;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRepository;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.relatorio.RelatorioService;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.VendaResponse;
import br.com.balcao.pdv.venda.VendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Recursos que deixam o PDV competitivo: desconto, fiado, balança, promoção, espera, tributos, NF-e de entrada. */
@SpringBootTest
class RecursosDeBalcaoIntegracaoTest extends IntegracaoBase {

    @Autowired CaixaService caixaService;
    @Autowired VendaService vendaService;
    @Autowired ProdutoService produtoService;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired ClienteService clienteService;
    @Autowired LojaService lojaService;
    @Autowired NfceService nfceService;
    @Autowired NotaFiscalRepository notaRepository;
    @Autowired ConfiguracaoFiscalService configuracaoFiscalService;
    @Autowired EntradaNfeService entradaNfeService;
    @Autowired RelatorioService relatorioService;
    @Autowired JdbcTemplate jdbc;

    private Produto refri;
    private Produto pao;

    @BeforeEach
    void preparar() {
        jdbc.execute(LIMPAR_BASE);
        jdbc.update("UPDATE configuracao_fiscal SET proximo_numero = 1");
        lojaService.atualizar(new LojaService.LojaRequest("Loja Teste", null, null, "#D9482B", null,
                PoliticaSaldoInsuficiente.PERMITIR_E_AVISAR, null, new BigDecimal("5"), "2", 4,
                TipoValorBalanca.PRECO, "pix@loja.com.br", "Loja Teste", "Sao Paulo", new BigDecimal("30")));
        configuracaoFiscalService.atualizar(new ConfiguracaoFiscalDto.Request(
                "11222333000181", "111111111111", "EMPRESA TESTE LTDA", "Teste", 1, "Rua A", "1", "Centro",
                "3550308", "São Paulo", "SP", "01001000", null, Ambiente.HOMOLOGACAO, 1, null, "1", "CSC-TESTE",
                "https://sefaz.exemplo/qrcode", "https://sefaz.exemplo/consulta", false, 30, TipoEmissor.SIMULADO));
        refri = produtoService.cadastrar(ProdutoRequest.basico("Refrigerante", new BigDecimal("10.00"), "001",
                "2000000000015", "UN", "22021000", null, new BigDecimal("50")));
        pao = produtoService.cadastrar(ProdutoRequest.basico("Pão (kg)", new BigDecimal("16.90"), "0015", null,
                "KG", "19059090", null, new BigDecimal("20")));
        caixaService.abrir(new BigDecimal("100.00"));
    }

    @Test
    void descontoPercentualRecalculaComOsItensEVaiRateadoParaONfce() {
        Long id = vendaService.iniciar().id();
        vendaService.adicionarItem(id, refri.getId(), null, new BigDecimal("3")); // 30,00
        VendaResponse v = vendaService.aplicarDesconto(id, null, new BigDecimal("10"), () -> true);
        assertThat(v.desconto()).isEqualByComparingTo("3.00");
        assertThat(v.total()).isEqualByComparingTo("27.00");

        v = vendaService.adicionarItem(id, pao.getId(), null, new BigDecimal("1")); // +16,90 → 46,90
        assertThat(v.desconto()).isEqualByComparingTo("4.69");
        assertThat(v.total()).isEqualByComparingTo("42.21");

        vendaService.adicionarPagamento(id, FormaPagamento.PIX, v.total(), null);
        vendaService.finalizar(id);
        nfceService.emitir(id);
        String xml = notaRepository.findFirstByVendaIdOrderByIdDesc(id).orElseThrow().getXml();
        // Rateio: 30,00/46,90 × 4,69 = 3,00 no refri; o pão leva o resto (1,69). Total do desconto confere.
        assertThat(xml).contains("<vDesc>3.00</vDesc>").contains("<vDesc>1.69</vDesc>")
                .contains("<ICMSTot>").contains("<vDesc>4.69</vDesc><vII>")
                .contains("<vProd>46.90</vProd><vFrete>").contains("<vNF>42.21</vNF>")
                .contains("<vTotTrib>12.66</vTotTrib>"); // 30% de 42,21
    }

    @Test
    void descontoAcimaDoLimiteDoOperadorPedeGerente() {
        Long id = vendaService.iniciar().id();
        vendaService.adicionarItem(id, refri.getId(), null, new BigDecimal("2"));

        assertThatThrownBy(() -> vendaService.aplicarDesconto(id, null, new BigDecimal("15"), () -> false))
                .extracting("codigo").isEqualTo("AUTORIZACAO_NECESSARIA");
        assertThat(vendaService.aplicarDesconto(id, new BigDecimal("1.00"), null, () -> false).total())
                .isEqualByComparingTo("19.00"); // 5% passa sem gerente
    }

    @Test
    void fiadoRespeitaOLimiteERecebimentoEmDinheiroEntraNoCaixa() {
        Cliente maria = clienteService.salvar(null, new ClienteService.ClienteRequest("Maria", "52998224725", null,
                null, new BigDecimal("50"), null, true));

        Long v1 = vendaService.iniciar().id();
        vendaService.adicionarItem(v1, refri.getId(), null, new BigDecimal("3"));
        assertThatThrownBy(() -> vendaService.adicionarPagamento(v1, FormaPagamento.CREDIARIO, new BigDecimal("30"), null))
                .extracting("codigo").isEqualTo("VENDA_SEM_CLIENTE");
        VendaResponse comCliente = vendaService.vincularCliente(v1, maria.getId());
        assertThat(comCliente.documentoConsumidor()).isEqualTo("52998224725");
        vendaService.adicionarPagamento(v1, FormaPagamento.CREDIARIO, new BigDecimal("30"), null);
        vendaService.finalizar(v1);
        assertThat(clienteService.buscar(maria.getId()).getSaldoDevedor()).isEqualByComparingTo("30.00");

        Long v2 = vendaService.iniciar().id();
        vendaService.adicionarItem(v2, refri.getId(), null, new BigDecimal("3"));
        vendaService.vincularCliente(v2, maria.getId());
        vendaService.adicionarPagamento(v2, FormaPagamento.CREDIARIO, new BigDecimal("30"), null);
        assertThatThrownBy(() -> vendaService.finalizar(v2))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("LIMITE_CREDITO_EXCEDIDO");

        var caixa = caixaService.exigirAberto();
        clienteService.receber(maria.getId(), new BigDecimal("10"), FormaPagamento.DINHEIRO, null, null);
        assertThat(clienteService.buscar(maria.getId()).getSaldoDevedor()).isEqualByComparingTo("20.00");
        assertThat(caixaService.saldoEsperado(caixaService.buscar(caixa.getId()))).isEqualByComparingTo("110.00");
    }

    @Test
    void etiquetaDeBalancaLancaOPesoPeloPrecoEmbutido() {
        Long id = vendaService.iniciar().id();
        // 2 | 0015 | 00 | 00845 | DV → pão, R$ 8,45 → 0,500 kg a R$ 16,90
        VendaResponse v = vendaService.adicionarItem(id, null, comDv("200150000845"), null);

        assertThat(v.itens()).singleElement().satisfies(i -> {
            assertThat(i.descricao()).isEqualTo("Pão (kg)");
            assertThat(i.quantidade()).isEqualByComparingTo("0.500");
            assertThat(i.subtotal()).isEqualByComparingTo("8.45");
        });
    }

    @Test
    void promocaoVigenteEntraSozinhaNaVenda() {
        produtoService.atualizar(refri.getId(), new ProdutoRequest("Refrigerante", new BigDecimal("10.00"), "001",
                "2000000000015", "UN", "22021000", "5102", 0, "102", null, null, null, new BigDecimal("6.00"),
                new BigDecimal("8.99"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), false, null));
        Long id = vendaService.iniciar().id();

        VendaResponse v = vendaService.adicionarItem(id, null, "2000000000015", new BigDecimal("2"));

        assertThat(v.itens().getFirst().promocional()).isTrue();
        assertThat(v.total()).isEqualByComparingTo("17.98");
    }

    @Test
    void vendaEmEsperaLiberaOCaixaParaOutroCliente() {
        Long primeira = vendaService.iniciar().id();
        vendaService.adicionarItem(primeira, refri.getId(), null, BigDecimal.ONE);
        vendaService.colocarEmEspera(primeira, "Moça do boné");

        Long segunda = vendaService.iniciar().id();
        assertThat(vendaService.emEspera()).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(primeira));

        vendaService.adicionarItem(segunda, pao.getId(), null, BigDecimal.ONE);
        vendaService.retomar(primeira);
        assertThat(vendaService.emAndamento().orElseThrow().id()).isEqualTo(primeira);
        assertThat(vendaService.emEspera()).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(segunda));
    }

    @Test
    void pixDaVendaSaiNoValorRestante() {
        Long id = vendaService.iniciar().id();
        vendaService.adicionarItem(id, refri.getId(), null, new BigDecimal("2"));
        vendaService.adicionarPagamento(id, FormaPagamento.DINHEIRO, new BigDecimal("5"), null);

        var pix = vendaService.pix(id, null);

        assertThat((BigDecimal) pix.get("valor")).isEqualByComparingTo("15.00");
        assertThat((String) pix.get("payload")).contains("540515.00").contains("pix@loja.com.br");
    }

    @Test
    void entradaPeloXmlDaNfeAtualizaCustoCriaProdutoEDaEntradaNoEstoque() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe"><NFe><infNFe Id="NFe35260911444777000161550010000012341000012345">
                <ide><serie>1</serie><nNF>1234</nNF></ide>
                <emit><CNPJ>11444777000161</CNPJ><xNome>DISTRIBUIDORA EXEMPLO</xNome></emit>
                <det nItem="1"><prod><cProd>A1</cProd><cEAN>2000000000015</cEAN><xProd>REFRIG COLA 2L</xProd>
                  <NCM>22021000</NCM><uCom>UN</uCom><qCom>12.0000</qCom><vUnCom>6.5000000000</vUnCom><vProd>78.00</vProd></prod></det>
                <det nItem="2"><prod><cProd>B7</cProd><cEAN>SEM GTIN</cEAN><xProd>SUCO UVA 1L</xProd>
                  <NCM>20096100</NCM><uCom>UN</uCom><qCom>6.0000</qCom><vUnCom>5.0000000000</vUnCom><vProd>30.00</vProd></prod></det>
                <total><ICMSTot><vNF>108.00</vNF></ICMSTot></total>
                </infNFe></NFe></nfeProc>""";

        EntradaNfeService.Previa previa = entradaNfeService.ler(xml);
        assertThat(previa.fornecedor()).isEqualTo("DISTRIBUIDORA EXEMPLO");
        assertThat(previa.itens()).hasSize(2);
        assertThat(previa.itens().get(0).produto().id()).isEqualTo(refri.getId());
        assertThat(previa.itens().get(1).produto()).isNull();
        assertThat(previa.itens().get(1).precoSugerido()).isEqualByComparingTo("6.99");

        var resultado = entradaNfeService.confirmar(List.of(
                new EntradaNfeService.ItemEntrada(refri.getId(), "2000000000015", "Refrigerante", "22021000", "UN",
                        new BigDecimal("12"), new BigDecimal("6.50"), null, false),
                new EntradaNfeService.ItemEntrada(null, null, "Suco de Uva 1L", "20096100", "UN",
                        new BigDecimal("6"), new BigDecimal("5.00"), new BigDecimal("7.49"), false)), "1234");

        assertThat(resultado.atualizados()).isEqualTo(1);
        assertThat(resultado.criados()).isEqualTo(1);
        Produto atualizado = produtoRepository.findById(refri.getId()).orElseThrow();
        assertThat(atualizado.getEstoqueAtual()).isEqualByComparingTo("62");
        assertThat(atualizado.getPrecoCusto()).isEqualByComparingTo("6.50");
        assertThat(produtoRepository.findAll()).anySatisfy(p -> {
            assertThat(p.getNome()).isEqualTo("Suco de Uva 1L");
            assertThat(p.getEstoqueAtual()).isEqualByComparingTo("6");
        });
    }

    @Test
    void painelSomaFaturamentoLucroEFormas() {
        produtoService.atualizar(refri.getId(), new ProdutoRequest("Refrigerante", new BigDecimal("10.00"), "001",
                "2000000000015", "UN", "22021000", "5102", 0, "102", null, null, null, new BigDecimal("6.00"),
                null, null, null, false, null));
        Long id = vendaService.iniciar().id();
        vendaService.adicionarItem(id, refri.getId(), null, new BigDecimal("2"));
        vendaService.adicionarPagamento(id, FormaPagamento.DINHEIRO, new BigDecimal("50"), null);
        vendaService.finalizar(id);

        var painel = relatorioService.painel(LocalDate.now());

        assertThat(painel.hoje().vendas()).isEqualTo(1);
        assertThat(painel.hoje().faturamento()).isEqualByComparingTo("20.00");
        assertThat(painel.hoje().lucroBruto()).isEqualByComparingTo("8.00");
        assertThat(painel.porForma()).singleElement().satisfies(s -> {
            assertThat(s.rotulo()).isEqualTo("DINHEIRO");
            assertThat(s.valor()).isEqualByComparingTo("20.00"); // 50 recebidos − 30 de troco
        });
        assertThat(relatorioService.curvaAbc(LocalDate.now(), LocalDate.now()).itens())
                .singleElement().satisfies(i -> assertThat(i.classe()).isEqualTo("A"));
    }

    /** Acrescenta o dígito verificador EAN-13. */
    private static String comDv(String doze) {
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (doze.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return doze + (10 - soma % 10) % 10;
    }
}
