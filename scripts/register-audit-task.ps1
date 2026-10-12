#requires -version 5.1
<#
.SYNOPSIS
    Registra/actualiza la Tarea Programada de Windows que corre la auditoría
    de seguridad local (scripts/audit-security.ps1).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\register-audit-task.ps1
    powershell -ExecutionPolicy Bypass -File scripts\register-audit-task.ps1 -DayOfWeek Friday -At 18:30
#>
[CmdletBinding()]
param(
    [string]$TaskName = 'ServicioAgua-SecurityAudit',
    [ValidateSet('Weekly', 'Daily')]
    [string]$Schedule = 'Weekly',
    [ValidateSet('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')]
    [string]$DayOfWeek = 'Monday',
    [string]$At = '09:00'
)

$ErrorActionPreference = 'Stop'

$audit = Join-Path $PSScriptRoot 'audit-security.ps1'
if (-not (Test-Path -LiteralPath $audit)) {
    throw "No se encontro el script de auditoria: $audit"
}

$accion = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$audit`""

$trigger = if ($Schedule -eq 'Daily') {
    New-ScheduledTaskTrigger -Daily -At $At
} else {
    New-ScheduledTaskTrigger -Weekly -DaysOfWeek $DayOfWeek -At $At
}

$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive -RunLevel Limited

$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable `
    -DontStopOnIdleEnd -ExecutionTimeLimit (New-TimeSpan -Hours 1)

Register-ScheduledTask -TaskName $TaskName -Action $accion -Trigger $trigger `
    -Principal $principal -Settings $settings -Force `
    -Description 'Auditoria de seguridad/auth del Sistema de Gestion de Agua Potable (invariantes + tests).' | Out-Null

$info = Get-ScheduledTask -TaskName $TaskName | Get-ScheduledTaskInfo
Write-Host "Tarea '$TaskName' registrada." -ForegroundColor Green
Write-Host ("Programacion: {0} {1}" -f $Schedule, $(if ($Schedule -eq 'Weekly') { "($DayOfWeek) $At" } else { $At }))
Write-Host ("Proxima ejecucion: {0}" -f $info.NextRunTime)
Write-Host "Para ejecutarla ahora: Start-ScheduledTask -TaskName '$TaskName'"
Write-Host "Para quitarla: Unregister-ScheduledTask -TaskName '$TaskName' -Confirm:`$false"
