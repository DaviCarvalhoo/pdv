# Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
# Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
# Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
# a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
# Autoria: BPDV-7F3A-DC26

# Sobe o banco e o servidor do Balcão PDV e mantém tudo no ar:
# se o servidor cair (erro, falta de memória, etc.), ele é reiniciado sozinho em 5 segundos.
# Uso: .\iniciar-balcao.ps1            (loja de verdade, banco vazio na primeira vez)
#      .\iniciar-balcao.ps1 -Demo      (loja de demonstração)
param([switch]$Demo)

$raiz = Split-Path $PSScriptRoot -Parent
$logs = Join-Path $raiz 'logs'
New-Item -ItemType Directory -Force $logs | Out-Null
$supervisor = Join-Path $logs 'supervisor.log'
function Registrar($texto) { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $texto" | Add-Content $supervisor }

# 1. Banco de dados (Docker). O contêiner volta sozinho após reiniciar o PC (restart: unless-stopped).
Set-Location $raiz
for ($i = 0; $i -lt 60; $i++) {
    docker info *> $null
    if ($LASTEXITCODE -eq 0) { break }
    if ($i -eq 0) { Registrar 'Aguardando o Docker iniciar...' }
    Start-Sleep -Seconds 5
}
docker compose up -d | Out-Null
Registrar 'Banco de dados no ar.'

# 2. Servidor, com reinício automático.
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$jar = Join-Path $raiz 'backend\target\balcao-pdv-1.0.0.jar'
if (-not (Test-Path $jar)) { Registrar 'Servidor não compilado: rode instalacao\compilar.ps1'; exit 1 }
$env:BALCAO_FRONTEND = Join-Path $raiz 'frontend\dist'
$env:CORS_ORIGINS = 'http://localhost:5173'
$argumentos = @('-Xms256m', '-Xmx768m', '-jar', $jar)
if ($Demo) { $argumentos += '--spring.profiles.active=demo' }

$ip = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
       Where-Object { $_.IPAddress -notmatch '^(127|169\.254)\.' } | Select-Object -First 1).IPAddress
Registrar "Sistema em http://localhost:8080 (outros caixas da rede: http://$($ip):8080)"

while ($true) {
    Registrar 'Iniciando o servidor...'
    $p = Start-Process -FilePath $java -ArgumentList $argumentos -WorkingDirectory "$raiz\backend" -NoNewWindow -PassThru `
        -RedirectStandardOutput (Join-Path $logs 'backend.log') -RedirectStandardError (Join-Path $logs 'backend-erro.log')
    $p.WaitForExit()
    Registrar "Servidor parou (código $($p.ExitCode)). Reiniciando em 5 s..."
    Start-Sleep -Seconds 5
}
