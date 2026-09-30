# Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
# Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
# Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
# a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
# Autoria: BPDV-7F3A-DC26

# Deixa o Balcão PDV ligando sozinho com o Windows e com backup diário às 23:30.
# Rode UMA vez, como administrador (botão direito > Executar com PowerShell como administrador).
param([switch]$Demo)
$ErrorActionPreference = 'Stop'
$aqui = $PSScriptRoot
$usuario = "$env:USERDOMAIN\$env:USERNAME"

$iniciar = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$aqui\iniciar-balcao.ps1`"" + $(if ($Demo) { ' -Demo' } else { '' })
Register-ScheduledTask -TaskName 'Balcao PDV' -Force `
    -Action (New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $iniciar) `
    -Trigger (New-ScheduledTaskTrigger -AtLogOn -User $usuario) `
    -Settings (New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit 0 `
               -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1)) | Out-Null

Register-ScheduledTask -TaskName 'Balcao PDV - backup' -Force `
    -Action (New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$aqui\backup.ps1`"") `
    -Trigger (New-ScheduledTaskTrigger -Daily -At '23:30') `
    -Settings (New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries) | Out-Null

# Libera a porta 8080 para os outros caixas da rede (tablet, segundo PC).
if (-not (Get-NetFirewallRule -DisplayName 'Balcao PDV' -ErrorAction SilentlyContinue)) {
    New-NetFirewallRule -DisplayName 'Balcao PDV' -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private | Out-Null
}

Write-Host 'Pronto: o Balcão PDV liga junto com o Windows, reinicia sozinho se cair e faz backup todo dia às 23:30.' -ForegroundColor Green
Write-Host 'Para desfazer: Unregister-ScheduledTask -TaskName "Balcao PDV*"' -ForegroundColor DarkGray
