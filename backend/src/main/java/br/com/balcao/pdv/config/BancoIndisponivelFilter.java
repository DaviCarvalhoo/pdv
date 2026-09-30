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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

/**
 * O banco caiu (energia, Docker reiniciando) com o servidor no ar. Em vez de um "erro 500" genérico, a tela
 * recebe 503 BANCO_INDISPONIVEL e trata como falta de conexão: mostra a faixa, espera e tenta de novo. Como a
 * operação roda numa transação, nada fica gravado pela metade.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BancoIndisponivelFilter extends OncePerRequestFilter {

    public static final String MENSAGEM =
            "O banco de dados não respondeu. Nada foi gravado; o sistema tenta de novo sozinho em instantes.";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | RuntimeException e) {
            if (!ehBancoIndisponivel(e) || response.isCommitted()) {
                throw e;
            }
            log.warn("Banco indisponível em {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader("Retry-After", "3");
            response.setContentType("application/problem+json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,"
                    + "\"codigo\":\"BANCO_INDISPONIVEL\",\"detail\":\"" + MENSAGEM + "\"}");
        }
    }

    public static boolean ehBancoIndisponivel(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof CannotCreateTransactionException || t instanceof DataAccessResourceFailureException
                    || t instanceof QueryTimeoutException || t instanceof SQLTransientConnectionException
                    || t instanceof ConnectException
                    || t instanceof org.hibernate.exception.JDBCConnectionException) {
                return true;
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
                return true; // classe 08: falha de conexão (PostgreSQL)
            }
        }
        return false;
    }
}
