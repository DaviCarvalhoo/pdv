/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.comum;

import br.com.balcao.pdv.produto.Gtin;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadoresTest {

    @ParameterizedTest
    @ValueSource(strings = {"2000000000015", "7891000315507", "96385074", "012345678905"})
    void gtinValido(String gtin) {
        assertThat(Gtin.valido(gtin)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000000000016", "7891000315500", "123", "abcdefghijklm"})
    void gtinInvalido(String gtin) {
        assertThat(Gtin.valido(gtin)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11222333000181", "11.222.333/0001-81"})
    void documentoValido(String documento) {
        assertThat(Documento.valido(documento)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "11222333000180", "00000000000000", "123"})
    void documentoInvalido(String documento) {
        assertThat(Documento.valido(documento)).isFalse();
    }
}
