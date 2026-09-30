package br.com.balcao.pdv.produto;

/** Validação do dígito verificador GTIN-8/12/13/14 (padrão GS1). */
public final class Gtin {

    private Gtin() {
    }

    public static boolean valido(String gtin) {
        if (gtin == null || !gtin.matches("\\d{8}|\\d{12,14}")) {
            return false;
        }
        int soma = 0;
        int n = gtin.length();
        // Da direita para a esquerda (sem o DV), os pesos alternam 3, 1, 3, 1...
        for (int i = n - 2; i >= 0; i--) {
            int digito = gtin.charAt(i) - '0';
            soma += ((n - 2 - i) % 2 == 0) ? digito * 3 : digito;
        }
        int dv = (10 - soma % 10) % 10;
        return dv == gtin.charAt(n - 1) - '0';
    }
}
