/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv;

import br.com.balcao.pdv.caixa.CaixaService;
import br.com.balcao.pdv.caixa.Terminal;
import br.com.balcao.pdv.caixa.TerminalRepository;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.produto.ProdutoRequest;
import br.com.balcao.pdv.produto.ProdutoService;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.VendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Vários pontos de venda na mesma loja, cada um com a sua gaveta. */
@SpringBootTest
class TerminalIntegracaoTest extends IntegracaoBase {

    @Autowired CaixaService caixaService;
    @Autowired VendaService vendaService;
    @Autowired ProdutoService produtoService;
    @Autowired TerminalRepository terminais;
    @Autowired JdbcTemplate jdbc;

    private Long caixa01;
    private Long caixa02;
    private Produto churros;

    @BeforeEach
    void preparar() {
        jdbc.execute(LIMPAR_BASE);
        jdbc.update("delete from terminal where id <> (select min(id) from terminal)");
        caixa01 = terminais.findFirstByAtivoTrueOrderByIdAsc().orElseThrow().getId();
        caixa02 = terminais.save(new Terminal("Caixa 02")).getId();
        churros = produtoService.cadastrar(ProdutoRequest.basico("Churros de doce de leite", new BigDecimal("8.00"),
                "10", null, "UN", "19059090", null, new BigDecimal("100")));
    }

    @Test
    void doisCaixasVendemAoMesmoTempoCadaUmNaSuaGaveta() {
        var c1 = caixaService.abrir(new BigDecimal("100.00"), null, caixa01);
        var c2 = caixaService.abrir(new BigDecimal("50.00"), null, caixa02);

        vendaDinheiro(caixa01, 2); // 16 na gaveta do Caixa 01
        vendaDinheiro(caixa02, 1); //  8 na gaveta do Caixa 02

        assertThat(caixaService.saldoEsperado(caixaService.buscar(c1.getId()))).isEqualByComparingTo("116.00");
        assertThat(caixaService.saldoEsperado(caixaService.buscar(c2.getId()))).isEqualByComparingTo("58.00");
        assertThat(caixaService.abertos()).hasSize(2);

        // Fechar um não mexe no outro.
        caixaService.fechar(c1.getId(), new BigDecimal("116.00"));
        assertThat(caixaService.aberto(caixa02)).isPresent();
        assertThat(caixaService.aberto(caixa01)).isEmpty();
    }

    @Test
    void mesmoTerminalNaoAbreDuasVezesEVendaNaoComecaComCaixaFechado() {
        caixaService.abrir(BigDecimal.TEN, null, caixa01);

        assertThatThrownBy(() -> caixaService.abrir(BigDecimal.TEN, null, caixa01))
                .extracting("codigo").isEqualTo("CAIXA_JA_ABERTO");
        assertThatThrownBy(() -> vendaService.iniciar(null, caixa02))
                .extracting("codigo").isEqualTo("CAIXA_NAO_ABERTO");
    }

    @Test
    void vendaEmAndamentoEhDoTerminal() {
        caixaService.abrir(BigDecimal.TEN, null, caixa01);
        caixaService.abrir(BigDecimal.TEN, null, caixa02);
        Long v1 = vendaService.iniciar(null, caixa01).id();
        Long v2 = vendaService.iniciar(null, caixa02).id();

        assertThat(vendaService.emAndamento(caixa01).orElseThrow().id()).isEqualTo(v1);
        assertThat(vendaService.emAndamento(caixa02).orElseThrow().id()).isEqualTo(v2);
    }

    private void vendaDinheiro(Long terminal, int quantidade) {
        Long v = vendaService.iniciar(null, terminal).id();
        var venda = vendaService.adicionarItem(v, churros.getId(), null, BigDecimal.valueOf(quantidade));
        vendaService.adicionarPagamento(v, FormaPagamento.DINHEIRO, venda.total(), null);
        vendaService.finalizar(v);
    }
}
