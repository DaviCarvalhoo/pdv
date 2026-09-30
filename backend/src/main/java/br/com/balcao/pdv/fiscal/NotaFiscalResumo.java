package br.com.balcao.pdv.fiscal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record NotaFiscalResumo(Long id, Long vendaId, Integer serie, Integer numero, String chaveAcesso,
                               Ambiente ambiente, StatusNota status, String protocolo, String motivo,
                               OffsetDateTime dataEmissao, OffsetDateTime dataAutorizacao,
                               OffsetDateTime dataCancelamento, BigDecimal valor) {

    public static NotaFiscalResumo de(NotaFiscal n) {
        return new NotaFiscalResumo(n.getId(), n.getVenda().getId(), n.getSerie(), n.getNumero(), n.getChaveAcesso(),
                n.getAmbiente(), n.getStatus(), n.getProtocolo(), n.getMotivo(), n.getDataEmissao(),
                n.getDataAutorizacao(), n.getDataCancelamento(), n.getVenda().getTotal());
    }
}
