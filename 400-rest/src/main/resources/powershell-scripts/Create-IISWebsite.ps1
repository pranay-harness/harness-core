# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

Import-Module WebAdministration

$releaseId="${workflow.ReleaseNo}"
$SiteName = "${serviceName}"
$SiteProtocol = "http"
$SitePort=80
$SiteHostName = "*"
$SitePhysicalDirectory = $env:SYSTEMDRIVE + "\Artifacts\" + $SiteName + "\release-" + $releaseId
$AppPoolName = "DefaultAppPool"

if (!(Test-Path IIS:\Sites\$SiteName -pathType container))
{
    $site = New-Item IIS:\Sites\$SiteName -physicalPath $sitePhysicalDirectory -bindings @{protocol=$SiteProtocol;bindingInformation=":"+$SitePort+":"+$SiteHostName} -ApplicationPool $AppPoolName -AutoStart $true
    Write-Host "Created Website:" $SiteName
}
else
{
    $site = Get-Item IIS:\Sites\$SiteName
    $Site | Set-ItemProperty -Name "physicalPath" -Value $sitePhysicalDirectory
    $Site | Set-ItemProperty -Name "bindings" -Value @{protocol=$SiteProtocol;bindingInformation=":"+$SitePort+":"+$SiteHostName}
    Write-Host "Updated Website:" $SiteName
}

Start-WebSite $SiteName
Get-Item IIS:\Sites\$SiteName
Write-Host "Done."
