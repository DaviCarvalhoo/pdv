package br.com.balcao.pdv.fiscal;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Chave de acesso de 44 dígitos: cUF(2) AAMM(4) CNPJ(14) mod(2) serie(3) nNF(9) tpEmis(1) cNF(8) cDV(1).
 */
public final class ChaveAcesso {

    private static final DateTimeFormatter AAMM = DateTimeFormatter.ofPattern("yyMM");

    private ChaveAcesso() {
    }

    public static String gerar(String uf, OffsetDateTime emissao, String cnpj, String modelo, int serie, int numero,
                               String tipoEmissao, String codigoNumerico) {
        String base = CodigoUf.de(uf)
                + emissao.format(AAMM)
                + cnpj
                + modelo
                + String.format("%03d", serie)
                + String.format("%09d", numero)
                + tipoEmissao
                + codigoNumerico;
        if (base.length() != 43 || !base.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Dados inválidos para a chave de acesso: " + base);
        }
        return base + digitoVerificador(base);
    }

    /** Módulo 11 com pesos de 2 a 9 da direita para a esquerda; resto 0 ou 1 resulta em 0. */
    public static int digitoVerificador(String base43) {
        int soma = 0;
        int peso = 2;
        for (int i = base43.length() - 1; i >= 0; i--) {
            soma += (base43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public static boolean valida(String chave) {
        return chave != null && chave.matches("\\d{44}")
                && digitoVerificador(chave.substring(0, 43)) == chave.charAt(43) - '0';
    }

    /** Formato impresso no DANFE: grupos de 4 dígitos. */
    public static String formatada(String chave) {
        return chave.replaceAll("(\\d{4})(?=\\d)", "$1 ");
    }
}
