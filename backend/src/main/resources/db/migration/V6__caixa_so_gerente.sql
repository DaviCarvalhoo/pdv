-- Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
-- Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
-- Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
-- a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
-- Autoria: BPDV-7F3A-DC26
-- Quem abre e fecha o caixa: por padrão, só gerente ou administrador (o operador pede o PIN do gerente).
ALTER TABLE loja ADD COLUMN caixa_so_gerente BOOLEAN NOT NULL DEFAULT TRUE;
