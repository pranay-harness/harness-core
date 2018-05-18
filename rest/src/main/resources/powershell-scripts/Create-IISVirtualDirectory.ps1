Import-Module WebAdministration

$siteName="Default Web Site"
$releaseId="${workflow.ReleaseNo}"
$virtualDirectoryName="${service.Name}"
$appPhysicalDirectory=$env:SYSTEMDRIVE + "\Artifacts\" + $virtualDirectoryName + "\release-" + $releaseId

Write-Host "Creating Virtual Directory" $virtualDirectoryName ".."
$VirtualDirPath = 'IIS:\Sites\' + $siteName + '\' + $virtualDirectoryName
New-Item -Path $VirtualDirPath -Type VirtualDirectory -PhysicalPath $appPhysicalDirectory -Force

Write-Host "Done."