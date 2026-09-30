package br.com.balcao.pdv.caixa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimentacaoCaixaResponse(Long id, TipoMovimentacaoCaixa tipo, BigDecimal valor, String descricao,
                                        Long vendaId, OffsetDateTime dataHora) {

    public static MovimentacaoCaixaResponse de(MovimentacaoCaixa m) {
        return new MovimentacaoCaixaResponse(m.getId(), m.getTipo(), m.getValor(), m.getDescricao(), m.getVendaId(),
                m.getDataHora());
    }
}
