package br.com.balcao.pdv.usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Rotas públicas de login. O restante de /api/** exige token (ver AuthInterceptor). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    public record PrimeiroAcessoRequest(@NotBlank @Size(max = 60) String nome, @NotBlank String pin) {
    }

    public record LoginRequest(@NotNull Long usuarioId, @NotBlank String pin) {
    }

    public record PinRequest(@NotBlank String pin) {
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of("precisaPrimeiroAcesso", service.precisaPrimeiroAcesso(),
                "operadores", service.operadoresAtivos());
    }

    @PostMapping("/primeiro-acesso")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Login primeiroAcesso(@Valid @RequestBody PrimeiroAcessoRequest req) {
        return service.primeiroAcesso(req.nome(), req.pin());
    }

    @PostMapping("/entrar")
    public AuthService.Login entrar(@Valid @RequestBody LoginRequest req) {
        return service.entrar(req.usuarioId(), req.pin());
    }

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest request) {
        service.sair(AuthInterceptor.token(request));
    }

    /** Gera uma autorização de uso único (2 min) a partir do PIN de um gerente. */
    @PostMapping("/autorizar")
    public Map<String, String> autorizar(@Valid @RequestBody PinRequest req) {
        return Map.of("autorizacao", service.autorizar(req.pin()));
    }

    @GetMapping("/operadores")
    public List<Operador> operadores() {
        return service.operadoresAtivos();
    }
}
