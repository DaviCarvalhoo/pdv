/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.usuario;

import br.com.balcao.pdv.comum.ConflitoException;
import br.com.balcao.pdv.comum.Exclusao;
import br.com.balcao.pdv.comum.NaoAutenticadoException;
import br.com.balcao.pdv.comum.NaoEncontradoException;
import br.com.balcao.pdv.comum.RegraNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login por PIN, sessões por token e autorizações pontuais de gerente ("senha do supervisor").
 */
@Slf4j
@Service
public class AuthService {

    static final Duration DURACAO_SESSAO = Duration.ofHours(12);
    static final Duration DURACAO_AUTORIZACAO = Duration.ofMinutes(2);
    private static final int MAX_TENTATIVAS = 5;

    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final Clock relogio;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);
    private final SecureRandom aleatorio = new SecureRandom();

    /** Autorizações de gerente: uso único, curtas, só em memória. */
    private final Map<String, Autorizacao> autorizacoes = new ConcurrentHashMap<>();
    /** Freio simples contra chute de PIN: tentativas erradas por usuário. */
    private final Map<Long, Integer> tentativas = new ConcurrentHashMap<>();

    public record Autorizacao(Operador autorizador, OffsetDateTime expiraEm) {
    }

    public record Login(String token, Operador operador, OffsetDateTime expiraEm) {
    }

    public AuthService(UsuarioRepository usuarios, SessaoRepository sessoes, Clock relogio, JdbcTemplate jdbc) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.relogio = relogio;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public boolean precisaPrimeiroAcesso() {
        return usuarios.count() == 0;
    }

    @Transactional(readOnly = true)
    public List<Operador> operadoresAtivos() {
        return usuarios.findByAtivoTrueOrderByNome().stream().map(Operador::de).toList();
    }

    @Transactional
    public Login primeiroAcesso(String nome, String pin) {
        if (!precisaPrimeiroAcesso()) {
            throw new ConflitoException("JA_CONFIGURADO", "O administrador já foi criado. Entre com o seu PIN.");
        }
        Usuario admin = usuarios.save(new Usuario(nome.trim(), Papel.ADMIN, hash(pin)));
        log.info("Primeiro acesso: administrador {} criado", admin.getNome());
        return abrirSessao(admin);
    }

    @Transactional
    public Login entrar(Long usuarioId, String pin) {
        Usuario u = usuarios.findById(usuarioId).filter(Usuario::isAtivo)
                .orElseThrow(() -> new NaoAutenticadoException("Operador não encontrado."));
        if (tentativas.getOrDefault(u.getId(), 0) >= MAX_TENTATIVAS) {
            throw new NaoAutenticadoException("PIN bloqueado após " + MAX_TENTATIVAS
                    + " tentativas. Peça a um administrador para redefinir.");
        }
        if (!confere(pin, u.getPinHash())) {
            tentativas.merge(u.getId(), 1, Integer::sum);
            throw new NaoAutenticadoException("PIN incorreto.");
        }
        tentativas.remove(u.getId());
        u.setUltimoAcesso(agora());
        return abrirSessao(u);
    }

    @Transactional
    public void sair(String token) {
        if (token != null) {
            sessoes.deleteById(token);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Operador> operadorDoToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return sessoes.findByTokenAndExpiraEmAfter(token, agora())
                .map(Sessao::getUsuario)
                .filter(Usuario::isAtivo)
                .map(Operador::de);
    }

    /** Gera uma autorização de uso único se o PIN for de um gerente ou administrador ativo. */
    @Transactional(readOnly = true)
    public String autorizar(String pin) {
        if (!StringUtils.hasText(pin)) {
            throw new RegraNegocioException("PIN_OBRIGATORIO", "Informe o PIN do gerente.");
        }
        Usuario gerente = usuarios.findByAtivoTrueAndPapelIn(List.of(Papel.GERENTE, Papel.ADMIN)).stream()
                .filter(u -> confere(pin, u.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new NaoAutenticadoException("PIN de gerente inválido."));
        String token = novoToken();
        autorizacoes.put(token, new Autorizacao(Operador.de(gerente), agora().plus(DURACAO_AUTORIZACAO)));
        return token;
    }

    /** Consome a autorização (uso único). */
    public Optional<Operador> consumirAutorizacao(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        Autorizacao a = autorizacoes.remove(token);
        if (a == null || a.expiraEm().isBefore(agora())) {
            return Optional.empty();
        }
        return Optional.of(a.autorizador());
    }

    // ------------------------------------------------------------------ Usuários (admin)

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findByExcluidoFalseOrderByAtivoDescNome();
    }

    @Transactional
    public Usuario criar(String nome, Papel papel, String pin) {
        validarNome(nome, 0L);
        return usuarios.save(new Usuario(nome.trim(), papel, hash(pin)));
    }

    @Transactional
    public Usuario atualizar(Long id, String nome, Papel papel, boolean ativo, Operador quem) {
        Usuario u = buscar(id);
        validarNome(nome, id);
        boolean perdeAdmin = u.getPapel() == Papel.ADMIN && (papel != Papel.ADMIN || !ativo);
        if (perdeAdmin && usuarios.countByAtivoTrueAndPapel(Papel.ADMIN) <= 1) {
            throw new RegraNegocioException("ULTIMO_ADMIN", "É preciso manter pelo menos um administrador ativo.");
        }
        if (quem != null && quem.id().equals(id) && !ativo) {
            throw new RegraNegocioException("DESATIVAR_A_SI_MESMO", "Você não pode desativar o próprio usuário.");
        }
        u.setNome(nome.trim());
        u.setPapel(papel);
        u.setAtivo(ativo);
        if (!ativo) {
            sessoes.encerrarDoUsuario(id);
        }
        return u;
    }

    @Transactional
    public void redefinirPin(Long id, String pin) {
        Usuario u = buscar(id);
        u.setPinHash(hash(pin));
        tentativas.remove(id);
        sessoes.encerrarDoUsuario(id);
    }

    /**
     * Exclui o operador. Sem histórico, é apagado; com vendas ou movimentações, é marcado como excluído,
     * perde o acesso e libera o nome para um novo cadastro.
     */
    @Transactional
    public Exclusao excluir(Long id, Operador quem) {
        Usuario u = buscar(id);
        if (quem != null && quem.id().equals(id)) {
            throw new RegraNegocioException("EXCLUIR_A_SI_MESMO", "Você não pode excluir o próprio usuário.");
        }
        if (u.getPapel() == Papel.ADMIN && u.isAtivo() && usuarios.countByAtivoTrueAndPapel(Papel.ADMIN) <= 1) {
            throw new RegraNegocioException("ULTIMO_ADMIN", "É preciso manter pelo menos um administrador ativo.");
        }
        sessoes.encerrarDoUsuario(id);
        Integer usos = jdbc.queryForObject("""
                select (select count(*) from venda where operador_id = ?)
                     + (select count(*) from caixa where operador_abertura_id = ? or operador_fechamento_id = ?)
                     + (select count(*) from movimentacao_caixa where operador_id = ?)
                     + (select count(*) from lancamento_cliente where operador_id = ?)
                     + (select count(*) from vale_troca where operador_id = ?)
                     + (select count(*) from devolucao where operador_id = ?)
                """, Integer.class, id, id, id, id, id, id, id);
        if (usos == null || usos == 0) {
            usuarios.delete(u);
            return Exclusao.apagado("Operador");
        }
        String nome = u.getNome() + " (excluído " + id + ")";
        u.setNome(nome.length() > 60 ? nome.substring(nome.length() - 60) : nome);
        u.setAtivo(false);
        u.setExcluido(true);
        return Exclusao.arquivado("Operador");
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void limparExpiradas() {
        sessoes.limparExpiradas(agora());
        autorizacoes.values().removeIf(a -> a.expiraEm().isBefore(agora()));
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário", id));
    }

    private void validarNome(String nome, Long id) {
        if (!StringUtils.hasText(nome)) {
            throw new RegraNegocioException("NOME_OBRIGATORIO", "Informe o nome.");
        }
        if (usuarios.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id)) {
            throw new ConflitoException("NOME_DUPLICADO", "Já existe um usuário chamado " + nome.trim() + ".");
        }
    }

    private Login abrirSessao(Usuario u) {
        OffsetDateTime expira = agora().plus(DURACAO_SESSAO);
        Sessao s = sessoes.save(new Sessao(novoToken(), u, agora(), expira));
        return new Login(s.getToken(), Operador.de(u), expira);
    }

    private String hash(String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new RegraNegocioException("PIN_INVALIDO", "O PIN deve ter de 4 a 6 números.");
        }
        return bcrypt.encode(pin);
    }

    private boolean confere(String pin, String hash) {
        return pin != null && bcrypt.matches(pin, hash);
    }

    private String novoToken() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(relogio);
    }
}
