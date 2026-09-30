/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

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
