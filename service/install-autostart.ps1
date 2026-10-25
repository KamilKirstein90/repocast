# Starts the RepoCast watcher in the background at every Windows login (no console window).
$ErrorActionPreference = "Stop"
$pythonw = Join-Path (Split-Path (Get-Command python).Source) "pythonw.exe"
$action = New-ScheduledTaskAction -Execute $pythonw -Argument "`"$PSScriptRoot\watcher.py`"" -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 5)
Register-ScheduledTask -TaskName "RepoCast Watcher" -Action $action -Trigger $trigger -Settings $settings -Force | Out-Null
Start-ScheduledTask -TaskName "RepoCast Watcher"
Write-Host "RepoCast Watcher registered and started. Log: $PSScriptRoot\logs\watcher.log"
Write-Host "Remove with: Unregister-ScheduledTask -TaskName 'RepoCast Watcher'"
