package br.com.balcao.pdv.usuario;

import br.com.balcao.pdv.comum.NaoAutenticadoException;
import br.com.balcao.pdv.comum.SemPermissaoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Exige sessão válida em /api/** e confere o {@link Requer} do endpoint. */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo) || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        Operador operador = authService.operadorDoToken(token(request))
                .orElseThrow(() -> new NaoAutenticadoException("Sessão expirada. Entre com o seu PIN."));
        request.setAttribute(Contexto.ATRIBUTO, operador);

        Requer requer = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(), Requer.class);
        if (requer == null) {
            requer = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), Requer.class);
        }
        if (requer != null && !operador.pode(requer.value())) {
            throw new SemPermissaoException("SEM_PERMISSAO",
                    "Esta área é restrita a " + requer.value().name().toLowerCase() + ".");
        }
        return true;
    }

    static String token(HttpServletRequest request) {
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }
}
