package br.com.balcao.pdv.caixa;

import java.math.BigDecimal;

public enum SituacaoConferencia {
    CONFERE,
    SOBRA,
    FALTA;

    public static SituacaoConferencia de(BigDecimal diferenca) {
        return switch (diferenca.signum()) {
            case 0 -> CONFERE;
            case 1 -> SOBRA;
            default -> FALTA;
        };
    }
}
