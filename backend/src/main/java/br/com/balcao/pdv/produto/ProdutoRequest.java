/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.produto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProdutoRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120) String nome,
        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2) BigDecimal preco,
        @Size(max = 30) String codigoInterno,
        @Pattern(regexp = "\\d{8}|\\d{12,14}", message = "GTIN deve ter 8, 12, 13 ou 14 dígitos") String gtin,
        @Size(max = 6) String unidade,
        @Pattern(regexp = "\\d{8}", message = "NCM deve ter 8 dígitos") String ncm,
        @Pattern(regexp = "\\d{4}", message = "CFOP deve ter 4 dígitos") String cfop,
        @Min(0) @Max(8) Integer origem,
        @Pattern(regexp = "\\d{3}", message = "CSOSN deve ter 3 dígitos") String csosn,
        @PositiveOrZero BigDecimal estoqueMinimo,
        @PositiveOrZero(message = "Estoque inicial não pode ser negativo") BigDecimal estoqueInicial,
        Long categoriaId,
        @PositiveOrZero(message = "Custo não pode ser negativo") @Digits(integer = 10, fraction = 2) BigDecimal precoCusto,
        @Positive(message = "Preço promocional deve ser maior que zero") @Digits(integer = 10, fraction = 2)
        BigDecimal precoPromocional,
        LocalDate promocaoInicio,
        LocalDate promocaoFim,
        Boolean atalhoRapido,
        @DecimalMin("0") @DecimalMax("100") BigDecimal aliquotaTributos) {

    /** Atalho para o cadastro básico (usado nos dados de demonstração e nos testes). */
    public static ProdutoRequest basico(String nome, BigDecimal preco, String codigoInterno, String gtin,
                                        String unidade, String ncm, BigDecimal estoqueMinimo,
                                        BigDecimal estoqueInicial) {
        return new ProdutoRequest(nome, preco, codigoInterno, gtin, unidade, ncm, "5102", 0, "102", estoqueMinimo,
                estoqueInicial, null, null, null, null, null, null, null);
    }
}
