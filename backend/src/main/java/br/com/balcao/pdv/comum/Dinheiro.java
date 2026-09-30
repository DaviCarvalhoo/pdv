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
