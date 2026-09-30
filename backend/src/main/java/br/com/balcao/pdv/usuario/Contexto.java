package br.com.balcao.pdv.usuario;

import br.com.balcao.pdv.comum.NaoAutenticadoException;
import br.com.balcao.pdv.comum.SemPermissaoException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Acesso ao operador da requisição e às checagens de permissão com "PIN do gerente".
 */
@Component
@RequiredArgsConstructor
public class Contexto {

    static final String ATRIBUTO = "balcao.operador";
    public static final String CABECALHO_AUTORIZACAO = "X-Autorizacao";

    private final AuthService authService;

    public Operador operador() {
        Object o = request().getAttribute(ATRIBUTO);
        if (o instanceof Operador operador) {
            return operador;
        }
        throw new NaoAutenticadoException("Entre com o seu PIN.");
    }

    public Long operadorId() {
        return operador().id();
    }

    /**
     * Libera a ação se o operador for gerente/admin; senão, exige uma autorização de gerente válida no cabeçalho
     * {@code X-Autorizacao}. Devolve quem autorizou.
     */
    public Operador exigirGerente(String acao) {
        Operador atual = operador();
        if (atual.pode(Papel.GERENTE)) {
            return atual;
        }
        return authService.consumirAutorizacao(request().getHeader(CABECALHO_AUTORIZACAO))
                .orElseThrow(() -> new SemPermissaoException("AUTORIZACAO_NECESSARIA",
                        acao + " precisa da autorização de um gerente.", Map.of("acao", acao)));
    }

    /** Versão para quando o limite depende do valor (ex.: desconto): só pede autorização se precisar. */
    public boolean temAutorizacaoDeGerente() {
        if (operador().pode(Papel.GERENTE)) {
            return true;
        }
        String token = request().getHeader(CABECALHO_AUTORIZACAO);
        return token != null && authService.consumirAutorizacao(token).isPresent();
    }

    private static HttpServletRequest request() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
