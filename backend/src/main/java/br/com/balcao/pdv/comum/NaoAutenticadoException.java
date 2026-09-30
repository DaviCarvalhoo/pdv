package br.com.balcao.pdv.comum;

/** Sem sessão válida (HTTP 401). */
public class NaoAutenticadoException extends RegraNegocioException {

    public NaoAutenticadoException(String mensagem) {
        super("NAO_AUTENTICADO", mensagem);
    }
}
