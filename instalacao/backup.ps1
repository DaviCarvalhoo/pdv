# Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
# Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
# Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
# a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
# Autoria: BPDV-7F3A-DC26

# Backup do banco de dados (roda sozinho todo dia se você usou instalar-inicializacao.ps1).
# Guarda os últimos 30 dias em backups\. Para restaurar, veja instalacao\LEIA-ME.md.
$raiz = Split-Path $PSScriptRoot -Parent
$pasta = Join-Path $raiz 'backups'
New-Item -ItemType Directory -Force $pasta | Out-Null
$arquivo = "balcao-$(Get-Date -Format 'yyyy-MM-dd_HHmm').dump"

docker exec balcao-postgres pg_dump -U balcao -Fc -f "/tmp/$arquivo" balcao
if ($LASTEXITCODE -ne 0) { Write-Error 'Falha no backup: o banco está no ar?'; exit 1 }
docker cp "balcao-postgres:/tmp/$arquivo" (Join-Path $pasta $arquivo) | Out-Null
docker exec balcao-postgres rm "/tmp/$arquivo" | Out-Null

Get-ChildItem $pasta -Filter 'balcao-*.dump' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | Remove-Item
Write-Host "Backup salvo em backups\$arquivo" -ForegroundColor Green
