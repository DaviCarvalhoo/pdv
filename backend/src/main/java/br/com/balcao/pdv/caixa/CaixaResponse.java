/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.caixa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CaixaResponse(Long id, StatusCaixa status, BigDecimal saldoInicial, BigDecimal saldoEsperado,
                            OffsetDateTime dataAbertura, OffsetDateTime dataFechamento, BigDecimal valorContado,
                            BigDecimal diferenca, SituacaoConferencia situacaoConferencia,
                            /* Preenchido quando a gaveta passou do limite da loja: hora de fazer sangria. */
                            BigDecimal alertaSangriaLimite) {

    public static CaixaResponse de(Caixa c, BigDecimal saldoEsperado) {
        return de(c, saldoEsperado, null);
    }

    public static CaixaResponse de(Caixa c, BigDecimal saldoEsperado, BigDecimal alertaSangriaLimite) {
        return new CaixaResponse(c.getId(), c.getStatus(), c.getSaldoInicial(), saldoEsperado, c.getDataAbertura(),
                c.getDataFechamento(), c.getValorContado(), c.getDiferenca(), c.getSituacaoConferencia(),
                alertaSangriaLimite);
    }
}
