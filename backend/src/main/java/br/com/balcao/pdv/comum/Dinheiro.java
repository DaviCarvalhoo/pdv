/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras únicas de arredondamento para valores monetários e quantidades. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private Dinheiro() {
    }

    public static BigDecimal valor(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(2, ARREDONDAMENTO);
    }

    public static BigDecimal quantidade(BigDecimal quantidade) {
        return quantidade.setScale(3, ARREDONDAMENTO);
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
