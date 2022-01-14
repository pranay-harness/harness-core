Import-Module WebAdministration

$AppPoolName = "DefaultAppPool"
$AppPoolDotNetVersion = "v4.0"

if (!(Test-Path IIS:\AppPools\$AppPoolName -pathType container))
{
    Write-Host "Creating AppPool:" $AppPoolName
    $appPool = New-Item IIS:\AppPools\$AppPoolName
}
else
{
    Write-Host "Updating AppPool:" $AppPoolName
    $appPool = Get-Item IIS:\AppPools\$AppPoolName
}

$appPool | Set-ItemProperty -Name "managedRuntimeVersion" -Value $AppPoolDotNetVersion

Get-Item IIS:\AppPools\$AppPoolName | ft
Write-Host "Done."