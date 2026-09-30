package br.com.balcao.pdv.comum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Padroniza todos os erros no formato Problem Details (RFC 9457) com um código de negócio. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NaoEncontradoException.class)
    ProblemDetail naoEncontrado(NaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    ProblemDetail naoAutenticado(NaoAutenticadoException e) {
        return problema(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(SemPermissaoException.class)
    ProblemDetail semPermissao(SemPermissaoException e) {
        return problema(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regra(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        p.setTitle("Requisição inválida");
        p.setProperty("codigo", "DADOS_INVALIDOS");
        p.setProperty("campos", campos);
        return p;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail corpoInvalido(Exception e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Formato da requisição inválido");
        p.setTitle("Requisição inválida");
        p.setProperty("codigo", "REQUISICAO_MAL_FORMADA");
        return p;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concorrencia(OptimisticLockingFailureException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O registro foi alterado por outra operação. Atualize e tente de novo.");
        p.setTitle("Conflito de concorrência");
        p.setProperty("codigo", "CONFLITO_CONCORRENCIA");
        return p;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operação viola uma restrição de unicidade ou integridade.");
        p.setTitle("Conflito de dados");
        p.setProperty("codigo", "VIOLACAO_INTEGRIDADE");
        return p;
    }

    private ProblemDetail problema(HttpStatus status, RegraNegocioException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        p.setTitle(status.getReasonPhrase());
        p.setProperty("codigo", e.getCodigo());
        e.getDetalhes().forEach(p::setProperty);
        return p;
    }
}
