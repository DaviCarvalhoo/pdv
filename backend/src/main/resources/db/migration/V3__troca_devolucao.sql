-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Troca e devolução com vale-troca -----------------------------------------------------------
ALTER TABLE item_venda
    ADD COLUMN quantidade_devolvida NUMERIC(12, 3) NOT NULL DEFAULT 0;

CREATE TABLE vale_troca (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(12)    NOT NULL UNIQUE,
    valor           NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    saldo           NUMERIC(12, 2) NOT NULL CHECK (saldo >= 0),
    venda_origem_id BIGINT         REFERENCES venda (id),
    cliente_id      BIGINT         REFERENCES cliente (id),
    operador_id     BIGINT         REFERENCES usuario (id),
    criado_em       TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE devolucao (
    id             BIGSERIAL PRIMARY KEY,
    venda_id       BIGINT         NOT NULL REFERENCES venda (id),
    valor          NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    destino        VARCHAR(12)    NOT NULL,
    vale_troca_id  BIGINT         REFERENCES vale_troca (id),
    motivo         VARCHAR(255),
    operador_id    BIGINT         REFERENCES usuario (id),
    data_hora      TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_devolucao_venda ON devolucao (venda_id);
