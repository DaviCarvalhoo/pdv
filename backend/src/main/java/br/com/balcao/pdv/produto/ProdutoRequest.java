package br.com.balcao.pdv.produto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

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
        @PositiveOrZero(message = "Estoque inicial não pode ser negativo") BigDecimal estoqueInicial) {
}
