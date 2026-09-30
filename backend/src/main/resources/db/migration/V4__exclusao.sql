-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Exclusão de cadastros ---------------------------------------------------------------------
-- Cadastro sem histórico é apagado de verdade. Com histórico (vendas, movimentações), é marcado como
-- excluído: some das telas, libera nome/código/documento para reuso e preserva o passado.
ALTER TABLE usuario ADD COLUMN excluido BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produto ADD COLUMN excluido BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cliente ADD COLUMN excluido BOOLEAN NOT NULL DEFAULT FALSE;
