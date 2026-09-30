package br.com.balcao.pdv.caixa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CaixaResponse(Long id, StatusCaixa status, BigDecimal saldoInicial, BigDecimal saldoEsperado,
                            OffsetDateTime dataAbertura, OffsetDateTime dataFechamento, BigDecimal valorContado,
                            BigDecimal diferenca, SituacaoConferencia situacaoConferencia) {

    public static CaixaResponse de(Caixa c, BigDecimal saldoEsperado) {
        return new CaixaResponse(c.getId(), c.getStatus(), c.getSaldoInicial(), saldoEsperado, c.getDataAbertura(),
                c.getDataFechamento(), c.getValorContado(), c.getDiferenca(), c.getSituacaoConferencia());
    }
}
