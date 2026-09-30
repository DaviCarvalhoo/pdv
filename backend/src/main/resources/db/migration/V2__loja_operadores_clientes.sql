-- Loja (registro único) -----------------------------------------------------------
CREATE TABLE loja (
    id                      BIGINT PRIMARY KEY,
    nome_fantasia           VARCHAR(60)    NOT NULL DEFAULT 'Minha Loja',
    slogan                  VARCHAR(120),
    logo                    TEXT,
    cor_destaque            VARCHAR(7)     NOT NULL DEFAULT '#D9482B',
    mensagem_cupom          VARCHAR(255),
    politica_estoque        VARCHAR(20)    NOT NULL DEFAULT 'PERMITIR_E_AVISAR',
    limite_gaveta           NUMERIC(12, 2),
    desconto_max_operador   NUMERIC(5, 2)  NOT NULL DEFAULT 5,
    balanca_prefixo         VARCHAR(1)     NOT NULL DEFAULT '2',
    balanca_digitos_codigo  INTEGER        NOT NULL DEFAULT 4,
    balanca_tipo_valor      VARCHAR(5)     NOT NULL DEFAULT 'PRECO',
    atualizado_em           TIMESTAMPTZ    NOT NULL DEFAULT now()
);
INSERT INTO loja (id) VALUES (1);

-- Operadores ---------------------------------------------------------------------------
CREATE TABLE usuario (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(60)  NOT NULL UNIQUE,
    papel          VARCHAR(10)  NOT NULL,
    pin_hash       VARCHAR(100) NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_acesso  TIMESTAMPTZ
);

CREATE TABLE sessao (
    token       VARCHAR(64) PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id),
    criada_em   TIMESTAMPTZ NOT NULL,
    expira_em   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_sessao_usuario ON sessao (usuario_id);

-- Categorias e novos campos de produto -----------------------------------------------------
CREATE TABLE categoria (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(40) NOT NULL UNIQUE,
    cor   VARCHAR(7)
);

ALTER TABLE produto
    ADD COLUMN categoria_id      BIGINT REFERENCES categoria (id),
    ADD COLUMN preco_custo       NUMERIC(12, 2),
    ADD COLUMN preco_promocional NUMERIC(12, 2),
    ADD COLUMN promocao_inicio   DATE,
    ADD COLUMN promocao_fim      DATE;

ALTER TABLE item_venda
    ADD COLUMN custo_unitario NUMERIC(12, 2),
    ADD COLUMN promocional    BOOLEAN NOT NULL DEFAULT FALSE;

-- Clientes e fiado -------------------------------------------------------------------------
CREATE TABLE cliente (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(80)    NOT NULL,
    documento       VARCHAR(14)    UNIQUE,
    telefone        VARCHAR(20),
    email           VARCHAR(120),
    limite_credito  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_devedor   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    observacao      VARCHAR(255),
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version         BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX idx_cliente_nome ON cliente (lower(nome));

-- Vendas e caixa: operador, cliente e desconto ---------------------------------------------
ALTER TABLE venda
    ADD COLUMN operador_id         BIGINT REFERENCES usuario (id),
    ADD COLUMN cliente_id          BIGINT REFERENCES cliente (id),
    ADD COLUMN subtotal            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN desconto            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN desconto_percentual NUMERIC(5, 2);
UPDATE venda SET subtotal = total;
CREATE INDEX idx_venda_finalizacao ON venda (data_finalizacao) WHERE status = 'FINALIZADA';

CREATE TABLE lancamento_cliente (
    id           BIGSERIAL PRIMARY KEY,
    cliente_id   BIGINT         NOT NULL REFERENCES cliente (id),
    tipo         VARCHAR(10)    NOT NULL,
    valor        NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    forma        VARCHAR(20),
    venda_id     BIGINT         REFERENCES venda (id),
    operador_id  BIGINT         REFERENCES usuario (id),
    observacao   VARCHAR(255),
    saldo_apos   NUMERIC(12, 2) NOT NULL,
    data_hora    TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_lancamento_cliente ON lancamento_cliente (cliente_id, data_hora);

ALTER TABLE caixa
    ADD COLUMN operador_abertura_id   BIGINT REFERENCES usuario (id),
    ADD COLUMN operador_fechamento_id BIGINT REFERENCES usuario (id);

ALTER TABLE movimentacao_caixa
    ADD COLUMN operador_id BIGINT REFERENCES usuario (id);

-- Recursos de balcão ------------------------------------------------------------------------
ALTER TABLE loja
    ADD COLUMN chave_pix          VARCHAR(77),
    ADD COLUMN pix_recebedor      VARCHAR(25),
    ADD COLUMN pix_cidade         VARCHAR(15),
    ADD COLUMN aliquota_tributos  NUMERIC(5, 2);

-- Atalho rápido no PDV (pão, cafezinho...) e alíquota aproximada de tributos (Lei 12.741/IBPT).
ALTER TABLE produto
    ADD COLUMN atalho_rapido      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN aliquota_tributos  NUMERIC(5, 2);

-- Venda em espera: o operador estaciona a venda e atende outro cliente.
ALTER TABLE venda
    ADD COLUMN em_espera        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN identificacao    VARCHAR(40),
    ADD COLUMN tributos_aprox   NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- Tipos novos (RECEBIMENTO_CLIENTE, VALE_ALIMENTACAO) precisam de mais espaço.
ALTER TABLE movimentacao_caixa ALTER COLUMN tipo TYPE VARCHAR(24);
ALTER TABLE pagamento ALTER COLUMN forma TYPE VARCHAR(20);
