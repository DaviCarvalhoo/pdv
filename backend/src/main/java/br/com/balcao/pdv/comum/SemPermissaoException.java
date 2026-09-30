package br.com.balcao.pdv.comum;

import java.util.Map;

/**
 * Operação proibida para o perfil atual (HTTP 403). Com o código {@code AUTORIZACAO_NECESSARIA}, o frontend
 * pede o PIN de um gerente e repete a chamada com o cabeçalho {@code X-Autorizacao}.
 */
public class SemPermissaoException extends RegraNegocioException {

    public SemPermissaoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public SemPermissaoException(String codigo, String mensagem, Map<String, Object> detalhes) {
        super(codigo, mensagem, detalhes);
    }
}
