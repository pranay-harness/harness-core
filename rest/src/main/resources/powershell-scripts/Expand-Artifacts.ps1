$serviceName="${service.Name}"
$releaseId="${workflow.ReleaseNo}"
$artifactFilename = $env:TEMP + "\" + $serviceName + "-release-" + $releaseId + ".zip"
$appPhysicalDirectory=$env:SYSTEMDRIVE + "\Artifacts\" + $serviceName + "\release-" + $releaseId

Write-Host "Extracting package from" $artifactFilename "to" $appPhysicalDirectory
Expand-Archive -Path $artifactFilename -DestinationPath $appPhysicalDirectory -Force
Write-Host "Done."