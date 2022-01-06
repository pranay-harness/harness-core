# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

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
