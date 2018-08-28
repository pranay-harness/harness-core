if (Get-Module -ListAvailable -Name Posh-SSH) {
    Write-Host "Posh-SSH Module exists"

} else {
    Write-Host "Module does not exist, Installing"
    Install-Module -Name Posh-SSH -SkipPublisherCheck
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