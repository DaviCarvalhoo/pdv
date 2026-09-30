package br.com.balcao.pdv.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService service;
    private final Contexto contexto;

    public record UsuarioRequest(@NotBlank @Size(max = 60) String nome, @NotNull Papel papel, String pin,
                                 Boolean ativo) {
    }

    public record PinRequest(@NotBlank String pin) {
    }

    public record UsuarioResponse(Long id, String nome, Papel papel, boolean ativo, OffsetDateTime criadoEm,
                                  OffsetDateTime ultimoAcesso) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getPapel(), u.isAtivo(), u.getCriadoEm(),
                    u.getUltimoAcesso());
        }
    }

    /** Quem está logado. */
    @GetMapping("/eu")
    public Operador eu() {
        return contexto.operador();
    }

    @GetMapping
    @Requer(Papel.ADMIN)
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping
    @Requer(Papel.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest req) {
        return UsuarioResponse.de(service.criar(req.nome(), req.papel(), req.pin()));
    }

    @PutMapping("/{id}")
    @Requer(Papel.ADMIN)
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return UsuarioResponse.de(service.atualizar(id, req.nome(), req.papel(),
                req.ativo() == null || req.ativo(), contexto.operador()));
    }

    @PutMapping("/{id}/pin")
    @Requer(Papel.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirPin(@PathVariable Long id, @Valid @RequestBody PinRequest req) {
        service.redefinirPin(id, req.pin());
    }
}
