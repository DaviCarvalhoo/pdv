/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
