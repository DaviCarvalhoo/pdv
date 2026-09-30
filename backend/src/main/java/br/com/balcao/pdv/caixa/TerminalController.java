/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.caixa;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Exclusao;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.usuario.Papel;
import br.com.balcao.pdv.usuario.Requer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Cadastro dos pontos de venda físicos da loja ("Caixa 01", "Caixa 02"...). */
@RestController
@RequestMapping("/api/terminais")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalRepository repository;
    private final CaixaRepository caixaRepository;
    private final CaixaService caixaService;
    private final JdbcTemplate jdbc;

    public record TerminalRequest(@NotBlank @Size(max = 40) String nome, Boolean ativo) {
    }

    /** O terminal e a gaveta dele agora: aberto por quem, desde quando, quanto tem. */
    public record TerminalResponse(Long id, String nome, boolean ativo, OffsetDateTime ultimoUso, Long caixaId,
                                   OffsetDateTime abertoEm, BigDecimal gaveta) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TerminalResponse> listar() {
        return repository.findAllByOrderByAtivoDescNome().stream().map(this::resposta).toList();
    }

    @PostMapping
    @Requer(Papel.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TerminalResponse criar(@Valid @RequestBody TerminalRequest req) {
        exigirNomeUnico(req.nome(), 0L);
        return resposta(repository.save(new Terminal(req.nome().trim())));
    }

    @PutMapping("/{id}")
    @Requer(Papel.ADMIN)
    @Transactional
    public TerminalResponse atualizar(@PathVariable Long id, @Valid @RequestBody TerminalRequest req) {
        Terminal t = buscar(id);
        exigirNomeUnico(req.nome(), id);
        boolean ativo = req.ativo() == null || req.ativo();
        if (!ativo) {
            exigirFechado(t);
        }
        t.setNome(req.nome().trim());
        t.setAtivo(ativo);
        return resposta(t);
    }

    /** Sem caixas no histórico, apaga; com histórico, só desativa (os caixas antigos continuam no relatório). */
    @DeleteMapping("/{id}")
    @Requer(Papel.ADMIN)
    @Transactional
    public Exclusao excluir(@PathVariable Long id) {
        Terminal t = buscar(id);
        exigirFechado(t);
        Integer caixas = jdbc.queryForObject("select count(*) from caixa where terminal_id = ?", Integer.class, id);
        if (caixas == null || caixas == 0) {
            if (repository.findByAtivoTrueOrderByNome().size() <= 1 && t.isAtivo()) {
                throw new RegraNegocioException("ULTIMO_TERMINAL", "A loja precisa de pelo menos um caixa ativo.");
            }
            repository.delete(t);
            return Exclusao.apagado("Caixa");
        }
        t.setAtivo(false);
        return Exclusao.arquivado("Caixa");
    }

    private void exigirFechado(Terminal t) {
        caixaRepository.findFirstByStatusAndTerminalId(StatusCaixa.ABERTO, t.getId()).ifPresent(c -> {
            throw new ConflitoException("TERMINAL_COM_CAIXA_ABERTO",
                    t.getNome() + " está com o caixa aberto. Feche o caixa antes.");
        });
    }

    private void exigirNomeUnico(String nome, Long id) {
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id)) {
            throw new ConflitoException("NOME_DUPLICADO", "Já existe um caixa chamado " + nome.trim() + ".");
        }
    }

    private Terminal buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NaoEncontradoException("Terminal", id));
    }

    private TerminalResponse resposta(Terminal t) {
        return caixaRepository.findFirstByStatusAndTerminalId(StatusCaixa.ABERTO, t.getId())
                .map(c -> new TerminalResponse(t.getId(), t.getNome(), t.isAtivo(), t.getUltimoUso(), c.getId(),
                        c.getDataAbertura(), caixaService.saldoEsperado(c)))
                .orElseGet(() -> new TerminalResponse(t.getId(), t.getNome(), t.isAtivo(), t.getUltimoUso(), null,
                        null, null));
    }
}
