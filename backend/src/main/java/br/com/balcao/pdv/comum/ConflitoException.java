package br.com.balcao.pdv.comum;

import java.util.Map;

/** Operação incompatível com o estado atual do recurso (HTTP 409). */
public class ConflitoException extends RegraNegocioException {

    public ConflitoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public ConflitoException(String codigo, String mensagem, Map<String, Object> detalhes) {
        super(codigo, mensagem, detalhes);
    }
}
