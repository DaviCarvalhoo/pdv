package br.com.balcao.pdv.comum;

import lombok.Getter;

import java.util.Map;

/** Violação de regra de negócio (HTTP 422). */
@Getter
public class RegraNegocioException extends RuntimeException {

    private final String codigo;
    private final Map<String, Object> detalhes;

    public RegraNegocioException(String codigo, String mensagem) {
        this(codigo, mensagem, Map.of());
    }

    public RegraNegocioException(String codigo, String mensagem, Map<String, Object> detalhes) {
        super(mensagem);
        this.codigo = codigo;
        this.detalhes = detalhes;
    }
}
