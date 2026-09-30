package br.com.balcao.pdv.comum;

/** Recurso inexistente (HTTP 404). */
public class NaoEncontradoException extends RegraNegocioException {

    public NaoEncontradoException(String recurso, Object id) {
        super(recurso.toUpperCase() + "_NAO_ENCONTRADO", recurso + " não encontrado: " + id);
    }
}
