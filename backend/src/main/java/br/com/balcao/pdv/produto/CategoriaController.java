/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.produto;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository repository;

    public record CategoriaRequest(@NotBlank @Size(max = 40) String nome,
                                   @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Cor no formato #RRGGBB") String cor) {
    }

    @GetMapping
    public List<Categoria> listar() {
        return repository.findAllByOrderByNome();
    }

    @PostMapping
    @Requer(Papel.GERENTE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Categoria criar(@Valid @RequestBody CategoriaRequest req) {
        exigirNomeUnico(req.nome(), 0L);
        return repository.save(new Categoria(req.nome().trim(), req.cor()));
    }

    @PutMapping("/{id}")
    @Requer(Papel.GERENTE)
    @Transactional
    public Categoria atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        Categoria c = repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Categoria", id));
        exigirNomeUnico(req.nome(), id);
        c.setNome(req.nome().trim());
        c.setCor(req.cor());
        return c;
    }

    @DeleteMapping("/{id}")
    @Requer(Papel.GERENTE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException("CATEGORIA_EM_USO", "Há produtos nesta categoria. Mova-os antes de excluir.");
        }
    }

    private void exigirNomeUnico(String nome, Long id) {
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id)) {
            throw new ConflitoException("CATEGORIA_DUPLICADA", "Já existe a categoria " + nome.trim() + ".");
        }
    }
}
