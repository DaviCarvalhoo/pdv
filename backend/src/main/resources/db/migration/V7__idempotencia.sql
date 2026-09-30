-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Requisições já processadas ------------------------------------------------------------------
-- Cada ação da tela (lançar item, pagamento, sangria...) leva uma chave única. Se a rede cair e a tela reenviar
-- a mesma ação, o servidor devolve a resposta guardada em vez de executar de novo: nada é lançado em dobro.
-- A chave é gravada NA MESMA TRANSAÇÃO da operação: ou as duas ficam, ou nenhuma (queda de energia no meio).
CREATE TABLE requisicao_processada (
    chave       VARCHAR(64)  PRIMARY KEY,
    status_http INTEGER      NOT NULL,
    tipo        VARCHAR(100),
    corpo       TEXT,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_requisicao_criado ON requisicao_processada (criado_em);
