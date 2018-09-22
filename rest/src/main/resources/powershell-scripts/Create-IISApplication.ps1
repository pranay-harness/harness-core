Import-Module WebAdministration

$siteName="Default Web Site"
$releaseId="${workflow.ReleaseNo}"
$appName="${service.Name}"
$appPhysicalDirectory=$env:SYSTEMDRIVE + "\Artifacts\" + $appName + "\release-" + $releaseId

Write-Host "Creating Application" $appName ".."
$VirtualAppPath = 'IIS:\Sites\' + $siteName + '\' + $appName
New-Item -Path $VirtualAppPath -Type Application -PhysicalPath $appPhysicalDirectory -Force

Write-Host "Done."