# Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
# Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
# Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
# a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
# Autoria: BPDV-7F3A-DC26

# Gera a versão de produção: a tela (frontend/dist) e o servidor (backend/target/*.jar).
# Rode de novo sempre que atualizar o código (git pull).
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent

Write-Host '1/2 Tela (frontend)...' -ForegroundColor Cyan
Push-Location "$raiz\frontend"
npm ci --no-audit --no-fund
npm run build
Pop-Location

Write-Host '2/2 Servidor (backend)...' -ForegroundColor Cyan
Push-Location "$raiz\backend"
mvn -q -B package -DskipTests
Pop-Location

Write-Host 'Pronto. Agora rode instalacao\iniciar-balcao.ps1' -ForegroundColor Green
