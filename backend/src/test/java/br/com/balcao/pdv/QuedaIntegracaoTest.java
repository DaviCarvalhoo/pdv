/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.fiscal.Ambiente;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalDto;
import br.com.balcao.pdv.fiscal.ConfiguracaoFiscalService;
import br.com.balcao.pdv.fiscal.NotaFiscalRepository;
import br.com.balcao.pdv.fiscal.RecuperacaoNfce;
import br.com.balcao.pdv.fiscal.StatusNota;
import br.com.balcao.pdv.fiscal.TipoEmissor;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.usuario.AuthService;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.VendaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O que acontece quando a rede ou a energia caem no meio do trabalho. */
@SpringBootTest
@AutoConfigureMockMvc
class QuedaIntegracaoTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService authService;
    @Autowired CaixaService caixaService;
    @Autowired VendaService vendaService;
    @Autowired ProdutoService produtoService;
    @Autowired ConfiguracaoFiscalService configuracaoFiscalService;
    @Autowired RecuperacaoNfce recuperacaoNfce;
    @Autowired NotaFiscalRepository notaRepository;

    private Produto churros;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.execute(LIMPAR_BASE);
        jdbc.update("UPDATE configuracao_fiscal SET proximo_numero = 1, emissao_automatica = true");
        churros = produtoService.cadastrar(ProdutoRequest.basico("Churros de doce de leite", new BigDecimal("8.00"),
                null, "2000000000015", "UN", "19059090", null, BigDecimal.valueOf(50)));
        var gerente = authService.criar("Ana", Papel.GERENTE, "1234");
        token = json.readTree(mvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + gerente.getId() + ",\"pin\":\"1234\"}"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void reenvioDaMesmaAcaoNaoLancaEmDobro() throws Exception {
        caixaService.abrir(BigDecimal.TEN);
        long vendaId = vendaId();
        String item = "{\"produtoId\":" + churros.getId() + ",\"quantidade\":1}";

        // A resposta "se perdeu" na rede e a tela reenviou a mesma ação, com a mesma chave.
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            var r = mvc.perform(post("/api/vendas/" + vendaId + "/itens").header("Authorization", "Bearer " + token)
                    .header("X-Idempotencia", "item-1").contentType(MediaType.APPLICATION_JSON).content(item))
                    .andExpect(status().is2xxSuccessful());
            if (tentativa > 0) {
                r.andExpect(header().string("X-Idempotencia-Repetida", "true"));
            }
        }
        String pagamento = "{\"forma\":\"DINHEIRO\",\"valor\":10}";
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(post("/api/vendas/" + vendaId + "/pagamentos").header("Authorization", "Bearer " + token)
                    .header("X-Idempotencia", "pag-1").contentType(MediaType.APPLICATION_JSON).content(pagamento))
                    .andExpect(status().is2xxSuccessful());
        }

        var venda = vendaService.detalhar(vendaId);
        assertThat(venda.itens()).hasSize(1);
        assertThat(venda.total()).isEqualByComparingTo("8.00");
        assertThat(venda.valorPago()).isEqualByComparingTo("10.00");

        // Uma chave nova é uma ação nova: o segundo churros entra.
        mvc.perform(post("/api/vendas/" + vendaId + "/itens").header("Authorization", "Bearer " + token)
                .header("X-Idempotencia", "item-2").contentType(MediaType.APPLICATION_JSON).content(item))
                .andExpect(status().is2xxSuccessful());
        assertThat(vendaService.detalhar(vendaId).total()).isEqualByComparingTo("16.00");
    }

    @Test
    void erroNaoFicaGuardadoEOReenvioTentaDeNovo() throws Exception {
        caixaService.abrir(BigDecimal.TEN);
        long vendaId = vendaId();
        String invalido = "{\"produtoId\":" + churros.getId() + ",\"quantidade\":-1}";
        mvc.perform(post("/api/vendas/" + vendaId + "/itens").header("Authorization", "Bearer " + token)
                        .header("X-Idempotencia", "item-x").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("select count(*) from requisicao_processada", Long.class)).isZero();

        mvc.perform(post("/api/vendas/" + vendaId + "/itens").header("Authorization", "Bearer " + token)
                        .header("X-Idempotencia", "item-x").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":" + churros.getId() + ",\"quantidade\":2}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().doesNotExist("X-Idempotencia-Repetida"));
        assertThat(vendaService.detalhar(vendaId).total()).isEqualByComparingTo("16.00");
    }

    @Test
    void vendaFinalizadaQueFicouSemNotaEhEmitidaAoReligar() {
        configurarEmitente();
        caixaService.abrir(BigDecimal.TEN);
        Long vendaId = vendaService.iniciar().id();
        vendaService.adicionarItem(vendaId, churros.getId(), null, BigDecimal.ONE);
        vendaService.adicionarPagamento(vendaId, FormaPagamento.PIX, new BigDecimal("8.00"), null);
        // A venda foi gravada e a energia caiu antes da emissão da nota.
        vendaService.finalizar(vendaId);
        assertThat(recuperacaoNfce.contarPendentes()).isEqualTo(1);

        assertThat(recuperacaoNfce.recuperar()).isEqualTo(1);

        assertThat(notaRepository.findFirstByVendaIdOrderByIdDesc(vendaId).orElseThrow().getStatus())
                .isEqualTo(StatusNota.AUTORIZADA);
        assertThat(recuperacaoNfce.contarPendentes()).isZero();
        assertThat(recuperacaoNfce.recuperar()).isZero(); // não emite de novo
        assertThat(notaRepository.count()).isEqualTo(1);
    }

    @Test
    void conferenciaAvisaCaixaDeOntemERelogioAtrasado() throws Exception {
        var caixa = caixaService.abrir(BigDecimal.TEN);
        mvc.perform(get("/api/sistema/saude").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avisos").isEmpty());

        // A luz caiu à noite e ninguém fechou o caixa.
        jdbc.update("update caixa set data_abertura = now() - interval '1 day 2 hours' where id = ?", caixa.getId());
        mvc.perform(get("/api/sistema/saude").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.avisos[*].codigo", hasItem("CAIXA_DE_OUTRO_DIA")));

        // O relógio do PC voltou no tempo: já existe venda "do futuro".
        Long vendaId = vendaService.iniciar().id();
        jdbc.update("update venda set data_abertura = now() + interval '1 day' where id = ?", vendaId);
        mvc.perform(get("/api/sistema/saude").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.avisos[*].codigo", hasItem("RELOGIO_ATRASADO")));
    }

    private long vendaId() throws Exception {
        JsonNode venda = json.readTree(mvc.perform(post("/api/vendas").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return venda.get("id").asLong();
    }

    private void configurarEmitente() {
        configuracaoFiscalService.atualizar(new ConfiguracaoFiscalDto.Request(
                "11222333000181", "111111111111", "EMPRESA TESTE LTDA", "Teste", 1, "Rua A", "1", "Centro",
                "3550308", "São Paulo", "SP", "01001000", null, Ambiente.HOMOLOGACAO, 1, null, "1", "CSC-TESTE",
                "https://sefaz.exemplo/qrcode", "https://sefaz.exemplo/consulta", true, 30, TipoEmissor.SIMULADO));
    }
}
