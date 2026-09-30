/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.fiscal;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ChaveAcessoTest {

    private static final OffsetDateTime SETEMBRO_2026 = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

    @Test
    void geraChaveCom44DigitosNaOrdemDoLayout() {
        String chave = ChaveAcesso.gerar("SP", SETEMBRO_2026, "11222333000181", "65", 1, 1, "1", "00000001");

        assertThat(chave).isEqualTo("35" + "2609" + "11222333000181" + "65" + "001" + "000000001" + "1" + "00000001" + "3");
        assertThat(ChaveAcesso.valida(chave)).isTrue();
    }

    @Test
    void digitoVerificadorModulo11() {
        assertThat(ChaveAcesso.digitoVerificador("3526091122233300018165001000000001100000001")).isEqualTo(3);
    }

    @Test
    void detectaChaveAdulterada() {
        String chave = ChaveAcesso.gerar("SP", SETEMBRO_2026, "11222333000181", "65", 1, 42, "1", "12345678");
        String adulterada = chave.substring(0, 30) + (chave.charAt(30) == '9' ? '0' : '9') + chave.substring(31);

        assertThat(ChaveAcesso.valida(adulterada)).isFalse();
    }

    @Test
    void formataEmGruposDeQuatro() {
        assertThat(ChaveAcesso.formatada("35260911222333000181650010000000011000000013"))
                .isEqualTo("3526 0911 2223 3300 0181 6500 1000 0000 0110 0000 0013");
    }

    @Test
    void qrCodeVersao2ComHashDoCsc() {
        String url = QrCodeNfce.url("https://sefaz.exemplo/qrcode", "35260911222333000181650010000000011000000013",
                Ambiente.HOMOLOGACAO, "000001", "CSC123");

        assertThat(url).isEqualTo("https://sefaz.exemplo/qrcode?p=35260911222333000181650010000000011000000013"
                + "|2|2|1|3E42BFBFB810F3A8955868C3D967E08AE202C132");
    }
}
