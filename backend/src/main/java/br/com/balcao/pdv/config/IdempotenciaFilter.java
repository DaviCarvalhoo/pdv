/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Torna as ações da tela à prova de reenvio. A tela manda um cabeçalho {@code X-Idempotencia} (uma chave por
 * ação) e, se a rede cair, reenvia a MESMA chave. Aqui:
 * <ul>
 *   <li>a chave e a operação são gravadas numa transação só: queda de energia no meio desfaz as duas, e o
 *       reenvio executa do zero; depois do commit, o reenvio recebe a resposta guardada, sem repetir nada;</li>
 *   <li>respostas de erro (4xx/5xx) não são guardadas nem gravam nada: o reenvio tenta de novo normalmente.</li>
 * </ul>
 * Finalização da venda e emissão de NFC-e ficam de fora: elas já são idempotentes por natureza (a segunda
 * finalização responde que a venda não está mais aberta, e a tela confere a venda) e emitem a nota numa
 * transação própria.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotenciaFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Idempotencia";
    private static final Set<String> METODOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public IdempotenciaFilter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        String chave = request.getHeader(CABECALHO);
        return chave == null || chave.isBlank() || chave.length() > 64
                || !METODOS.contains(request.getMethod())
                || !caminho.startsWith("/api/") || caminho.startsWith("/api/auth/")
                || caminho.endsWith("/finalizar") || caminho.endsWith("/nfce");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String chave = request.getHeader(CABECALHO).trim();

        // Já processada? Devolve a mesma resposta, sem executar de novo.
        List<Map<String, Object>> anterior = jdbc.queryForList(
                "select status_http, tipo, corpo from requisicao_processada where chave = ?", chave);
        if (!anterior.isEmpty()) {
            responder(response, anterior.getFirst());
            log.info("Reenvio {} {} já processado (chave {}): resposta repetida", request.getMethod(),
                    request.getRequestURI(), chave);
            return;
        }

        ContentCachingResponseWrapper resposta = new ContentCachingResponseWrapper(response);
        Excecao erro = new Excecao();
        try {
            transacao.executeWithoutResult(status -> {
                try {
                    chain.doFilter(request, resposta);
                } catch (IOException | ServletException e) {
                    erro.causa = e;
                    status.setRollbackOnly();
                    return;
                }
                if (resposta.getStatus() >= 400) {
                    // Erro de negócio ou de validação: nada deve ser gravado, e o reenvio tenta de novo.
                    status.setRollbackOnly();
                    return;
                }
                jdbc.update("insert into requisicao_processada (chave, status_http, tipo, corpo) values (?, ?, ?, ?)",
                        chave, resposta.getStatus(), resposta.getContentType(),
                        new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8));
            });
        } catch (DuplicateKeyException e) {
            // Dois reenvios chegaram juntos e o outro terminou primeiro: esta tentativa foi desfeita.
            List<Map<String, Object>> vencedor = jdbc.queryForList(
                    "select status_http, tipo, corpo from requisicao_processada where chave = ?", chave);
            resposta.resetBuffer();
            if (!vencedor.isEmpty()) {
                responder(response, vencedor.getFirst());
                return;
            }
        }
        if (erro.causa instanceof ServletException se) {
            throw se;
        }
        if (erro.causa instanceof IOException ioe) {
            throw ioe;
        }
        resposta.copyBodyToResponse();
    }

    /** Guarda 2 dias de chaves: bem mais que qualquer reenvio. */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void limpar() {
        jdbc.update("delete from requisicao_processada where criado_em < now() - interval '2 days'");
    }

    private static void responder(HttpServletResponse response, Map<String, Object> linha) throws IOException {
        response.setStatus(((Number) linha.get("status_http")).intValue());
        if (linha.get("tipo") != null) {
            response.setContentType((String) linha.get("tipo"));
        }
        response.setHeader("X-Idempotencia-Repetida", "true");
        String corpo = (String) linha.get("corpo");
        if (corpo != null && !corpo.isEmpty()) {
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
    }

    private static final class Excecao {
        Exception causa;
    }
}
