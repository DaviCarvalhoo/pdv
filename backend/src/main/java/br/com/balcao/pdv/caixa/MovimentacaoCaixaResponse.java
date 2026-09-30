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

public record MovimentacaoCaixaResponse(Long id, TipoMovimentacaoCaixa tipo, BigDecimal valor, String descricao,
                                        Long vendaId, OffsetDateTime dataHora) {

    public static MovimentacaoCaixaResponse de(MovimentacaoCaixa m) {
        return new MovimentacaoCaixaResponse(m.getId(), m.getTipo(), m.getValor(), m.getDescricao(), m.getVendaId(),
                m.getDataHora());
    }
}
