package br.com.balcao.pdv.loja;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LojaUtilTest {

    /** Exemplo oficial do Manual de Padrões para Iniciação do Pix (Banco Central). */
    @Test
    void brCodeIgualAoExemploDoBancoCentral() {
        String payload = PixBrCode.gerar("123e4567-e12b-12d1-a456-426655440000", "Fulano de Tal", "BRASILIA",
                null, null);

        assertThat(payload).isEqualTo("00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D");
    }

    @Test
    void brCodeComValorERemoveAcentos() {
        String payload = PixBrCode.gerar("pix@loja.com.br", "Padaria São João", "São Paulo",
                new BigDecimal("27.5"), "VENDA 42");

        assertThat(payload).contains("540527.50").contains("5916Padaria Sao Joao").contains("6009Sao Paulo")
                .contains("0507VENDA42");
        String semCrc = payload.substring(0, payload.length() - 4);
        assertThat(payload.substring(payload.length() - 4)).isEqualTo(PixBrCode.crc16(semCrc));
    }

    @Test
    void etiquetaDeBalancaComPreco() {
        var leitura = EtiquetaBalanca.ler(comDv("200150000845"), "2", 4, TipoValorBalanca.PRECO).orElseThrow();

        assertThat(leitura.codigoProduto()).isEqualTo("0015");
        assertThat(leitura.valor()).isEqualByComparingTo("8.45");
    }

    @Test
    void etiquetaDeBalancaComPeso() {
        var leitura = EtiquetaBalanca.ler(comDv("200150000350"), "2", 4, TipoValorBalanca.PESO).orElseThrow();

        assertThat(leitura.valor()).isEqualByComparingTo("0.350");
    }

    @Test
    void ignoraCodigoQueNaoEhDeBalanca() {
        assertThat(EtiquetaBalanca.ler("7891000315507", "2", 4, TipoValorBalanca.PRECO)).isEmpty();
        assertThat(EtiquetaBalanca.ler("2001500008450", "2", 4, TipoValorBalanca.PRECO)).isEmpty(); // DV errado
    }

    /** Acrescenta o dígito verificador EAN-13. */
    static String comDv(String doze) {
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (doze.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return doze + (10 - soma % 10) % 10;
    }
}
