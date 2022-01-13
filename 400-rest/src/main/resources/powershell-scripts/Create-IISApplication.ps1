# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

Import-Module WebAdministration

$siteName="Default Web Site"
$releaseId="${workflow.ReleaseNo}"
$appName="${serviceName}"
$appPhysicalDirectory=$env:SYSTEMDRIVE + "\Artifacts\" + $appName + "\release-" + $releaseId

Write-Host "Creating Application" $appName ".."
$VirtualAppPath = 'IIS:\Sites\' + $siteName + '\' + $appName
New-Item -Path $VirtualAppPath -Type Application -PhysicalPath $appPhysicalDirectory -Force

Write-Host "Done."
