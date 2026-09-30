-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Produtos -------------------------------------------------------------------
CREATE TABLE produto (
    id              BIGSERIAL PRIMARY KEY,
    codigo_interno  VARCHAR(30)    UNIQUE,
    gtin            VARCHAR(14)    UNIQUE,
    nome            VARCHAR(120)   NOT NULL,
    preco           NUMERIC(12, 2) NOT NULL CHECK (preco > 0),
    unidade         VARCHAR(6)     NOT NULL DEFAULT 'UN',
    ncm             VARCHAR(8),
    cfop            VARCHAR(4)     NOT NULL DEFAULT '5102',
    origem          INTEGER        NOT NULL DEFAULT 0,
    csosn           VARCHAR(3)     NOT NULL DEFAULT '102',
    estoque_atual   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    estoque_minimo  NUMERIC(12, 3),
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version         BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX idx_produto_nome ON produto (lower(nome));

-- Caixa ----------------------------------------------------------------------
CREATE TABLE caixa (
    id                    BIGSERIAL PRIMARY KEY,
    status                VARCHAR(10)    NOT NULL,
    saldo_inicial         NUMERIC(12, 2) NOT NULL CHECK (saldo_inicial >= 0),
    data_abertura         TIMESTAMPTZ    NOT NULL,
    data_fechamento       TIMESTAMPTZ,
    saldo_esperado        NUMERIC(12, 2),
    valor_contado         NUMERIC(12, 2),
    diferenca             NUMERIC(12, 2),
    situacao_conferencia  VARCHAR(10),
    version               BIGINT         NOT NULL DEFAULT 0
);
-- Garante no banco que só existe um caixa aberto por vez.
CREATE UNIQUE INDEX uk_caixa_unico_aberto ON caixa (status) WHERE status = 'ABERTO';

-- Vendas ---------------------------------------------------------------------
CREATE TABLE venda (
    id                   BIGSERIAL PRIMARY KEY,
    caixa_id             BIGINT         NOT NULL REFERENCES caixa (id),
    status               VARCHAR(12)    NOT NULL,
    total                NUMERIC(12, 2) NOT NULL DEFAULT 0,
    valor_pago           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    troco                NUMERIC(12, 2) NOT NULL DEFAULT 0,
    documento_consumidor VARCHAR(14),
    data_abertura        TIMESTAMPTZ    NOT NULL,
    data_finalizacao     TIMESTAMPTZ,
    data_cancelamento    TIMESTAMPTZ,
    motivo_cancelamento  VARCHAR(255),
    version              BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX idx_venda_caixa ON venda (caixa_id);
CREATE INDEX idx_venda_status_data ON venda (status, data_abertura);

CREATE TABLE item_venda (
    id               BIGSERIAL PRIMARY KEY,
    venda_id         BIGINT         NOT NULL REFERENCES venda (id) ON DELETE CASCADE,
    produto_id       BIGINT         NOT NULL REFERENCES produto (id),
    descricao        VARCHAR(120)   NOT NULL,
    preco_unitario   NUMERIC(12, 2) NOT NULL,
    quantidade       NUMERIC(12, 3) NOT NULL CHECK (quantidade > 0),
    subtotal         NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_item_venda_venda ON item_venda (venda_id);

CREATE TABLE pagamento (
    id                      BIGSERIAL PRIMARY KEY,
    venda_id                BIGINT         NOT NULL REFERENCES venda (id) ON DELETE CASCADE,
    forma                   VARCHAR(16)    NOT NULL,
    valor                   NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    identificador_transacao VARCHAR(60),
    data_hora               TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_pagamento_venda ON pagamento (venda_id);

-- Movimentações ----------------------------------------------------------------
CREATE TABLE movimentacao_caixa (
    id         BIGSERIAL PRIMARY KEY,
    caixa_id   BIGINT         NOT NULL REFERENCES caixa (id),
    tipo       VARCHAR(16)    NOT NULL,
    valor      NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    descricao  VARCHAR(255),
    venda_id   BIGINT         REFERENCES venda (id),
    data_hora  TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_mov_caixa_caixa ON movimentacao_caixa (caixa_id);

CREATE TABLE movimentacao_estoque (
    id              BIGSERIAL PRIMARY KEY,
    produto_id      BIGINT         NOT NULL REFERENCES produto (id),
    tipo            VARCHAR(16)    NOT NULL,
    quantidade      NUMERIC(12, 3) NOT NULL,
    saldo_anterior  NUMERIC(12, 3) NOT NULL,
    saldo_posterior NUMERIC(12, 3) NOT NULL,
    venda_id        BIGINT         REFERENCES venda (id),
    observacao      VARCHAR(255),
    data_hora       TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_mov_estoque_produto ON movimentacao_estoque (produto_id, data_hora);

-- Fiscal (NFC-e) ---------------------------------------------------------------
CREATE TABLE configuracao_fiscal (
    id                    BIGINT PRIMARY KEY,
    cnpj                  VARCHAR(14),
    inscricao_estadual    VARCHAR(14),
    razao_social          VARCHAR(60),
    nome_fantasia         VARCHAR(60),
    crt                   INTEGER      NOT NULL DEFAULT 1,
    logradouro            VARCHAR(60),
    numero                VARCHAR(10),
    bairro                VARCHAR(60),
    codigo_municipio      VARCHAR(7),
    municipio             VARCHAR(60),
    uf                    VARCHAR(2),
    cep                   VARCHAR(8),
    telefone              VARCHAR(14),
    ambiente              VARCHAR(12)  NOT NULL DEFAULT 'HOMOLOGACAO',
    serie                 INTEGER      NOT NULL DEFAULT 1,
    proximo_numero        INTEGER      NOT NULL DEFAULT 1,
    csc_id                VARCHAR(6),
    csc                   VARCHAR(36),
    url_qr_code           VARCHAR(255),
    url_consulta          VARCHAR(255),
    emissao_automatica    BOOLEAN      NOT NULL DEFAULT TRUE,
    prazo_cancelamento_min INTEGER     NOT NULL DEFAULT 30,
    emissor               VARCHAR(12)  NOT NULL DEFAULT 'SIMULADO'
);
INSERT INTO configuracao_fiscal (id) VALUES (1);

CREATE TABLE nota_fiscal (
    id                BIGSERIAL PRIMARY KEY,
    venda_id          BIGINT       NOT NULL REFERENCES venda (id),
    modelo            VARCHAR(2)   NOT NULL DEFAULT '65',
    serie             INTEGER      NOT NULL,
    numero            INTEGER      NOT NULL,
    chave_acesso      VARCHAR(44)  NOT NULL UNIQUE,
    ambiente          VARCHAR(12)  NOT NULL,
    status            VARCHAR(12)  NOT NULL,
    protocolo         VARCHAR(20),
    motivo            VARCHAR(255),
    data_emissao      TIMESTAMPTZ  NOT NULL,
    data_autorizacao  TIMESTAMPTZ,
    data_cancelamento TIMESTAMPTZ,
    justificativa_cancelamento VARCHAR(255),
    protocolo_cancelamento     VARCHAR(20),
    url_qr_code       TEXT,
    xml               TEXT         NOT NULL,
    UNIQUE (serie, numero, ambiente)
);
CREATE INDEX idx_nota_venda ON nota_fiscal (venda_id);
-- No máximo uma NFC-e autorizada por venda.
CREATE UNIQUE INDEX uk_nota_autorizada_por_venda ON nota_fiscal (venda_id) WHERE status = 'AUTORIZADA';
