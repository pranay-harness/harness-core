# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if (Get-Module -ListAvailable -Name Posh-SSH) {
    Write-Host "Posh-SSH Module exists"

} else {
    Write-Host "Module does not exist, Installing"
    Install-PackageProvider -Name NuGet -MinimumVersion 2.8.5.201 -Force
    Install-Module -Name Posh-SSH -SkipPublisherCheck -Force
}

Import-Module Posh-SSH

$server = "${serviceVariable.buildserver}"
$remotepath = "${serviceVariable.buildpath}"
$remotepath += "${workflow.variables.build}"
$localPath = $env:Temp
$username = "${serviceVariable.username}"
$password = "${serviceVariable.password}" | ConvertTo-SecureString -asPlainText -Force

$cred = New-Object System.Management.Automation.PSCredential($username,$password)

$session = New-SFTPSession -ComputerName $server -Credential $cred -AcceptKey

Get-SFTPFile $session -RemoteFile $remotepath -LocalPath $localPath -NoProgress
