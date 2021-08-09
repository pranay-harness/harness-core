$releaseId="${workflow.ReleaseNo}"
$serviceName="${serviceName}"
$artifactFilename = $env:TEMP + "\" + $serviceName + "-release-" + $releaseId + ".zip"

Write-Host "Starting Deployment [id:" $releaseId "]"
Write-Host "Downloading artifact file - URL: ${artifact.url} to File: " $artifactFilename
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri "${artifact.url}" -OutFile $artifactFilename
Write-Host "Done."