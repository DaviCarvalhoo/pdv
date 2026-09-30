package br.com.balcao.pdv;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Um PostgreSQL real (Testcontainers) compartilhado por todas as classes de integração. */
public abstract class IntegracaoBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static final String LIMPAR_BASE = "TRUNCATE devolucao, vale_troca, nota_fiscal, movimentacao_caixa, movimentacao_estoque, "
            + "pagamento, item_venda, venda, caixa, produto, lancamento_cliente, cliente, categoria, sessao, usuario "
            + "RESTART IDENTITY CASCADE";
}
