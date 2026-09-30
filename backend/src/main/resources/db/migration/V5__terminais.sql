-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Terminais de caixa ------------------------------------------------------------------------
-- Cada computador/tablet que vende é um terminal ("Caixa 01", "Caixa 02"...). Cada terminal tem a sua
-- gaveta: no máximo um caixa aberto POR TERMINAL, e vários terminais podem vender ao mesmo tempo.
CREATE TABLE terminal (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(40)  NOT NULL UNIQUE,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_uso  TIMESTAMPTZ
);
INSERT INTO terminal (nome) VALUES ('Caixa 01');

ALTER TABLE caixa ADD COLUMN terminal_id BIGINT REFERENCES terminal (id);
UPDATE caixa SET terminal_id = (SELECT min(id) FROM terminal);
ALTER TABLE caixa ALTER COLUMN terminal_id SET NOT NULL;

DROP INDEX uk_caixa_unico_aberto;
CREATE UNIQUE INDEX uk_caixa_aberto_por_terminal ON caixa (terminal_id) WHERE status = 'ABERTO';

-- Bloqueio de tela por inatividade (minutos; 0 desliga).
ALTER TABLE loja ADD COLUMN bloqueio_inatividade_min INTEGER NOT NULL DEFAULT 10;
