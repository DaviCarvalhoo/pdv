package br.com.balcao.pdv.loja;

import br.com.balcao.pdv.produto.Gtin;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Etiqueta de balança em EAN-13: {@code P CCCC[C][C] ... VVVVV D}.
 * <ul>
 *   <li>P: prefixo (normalmente 2, faixa GS1 de uso interno)</li>
 *   <li>C: código do produto (código interno), de 4 a 6 dígitos logo após o prefixo</li>
 *   <li>V: os 5 dígitos antes do verificador trazem o preço total (centavos) ou o peso (gramas)</li>
 *   <li>D: dígito verificador EAN-13</li>
 * </ul>
 * Cobre os formatos mais comuns das balanças Toledo, Filizola e Urano.
 */
public final class EtiquetaBalanca {

    private EtiquetaBalanca() {
    }

    public record Leitura(String codigoProduto, TipoValorBalanca tipo, BigDecimal valor) {
    }

    public static Optional<Leitura> ler(String codigo, String prefixo, int digitosCodigo, TipoValorBalanca tipo) {
        if (codigo == null || codigo.length() != 13 || !codigo.startsWith(prefixo) || !Gtin.valido(codigo)) {
            return Optional.empty();
        }
        String produto = codigo.substring(1, 1 + digitosCodigo);
        int embutido = Integer.parseInt(codigo.substring(7, 12));
        BigDecimal valor = tipo == TipoValorBalanca.PRECO
                ? BigDecimal.valueOf(embutido, 2)   // centavos → reais
                : BigDecimal.valueOf(embutido, 3);  // gramas → kg
        if (valor.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Leitura(produto, tipo, valor));
    }
}
