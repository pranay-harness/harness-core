$serviceName="${serviceName}"
$releaseId="${workflow.ReleaseNo}"
#$artifactFilename = $env:TEMP + "\" + $serviceName + "-release-" + $releaseId + ".zip"
$artifactFilename = "${DownloadDirectory}" + "\" + "${artifact.fileName}"
$appPhysicalDirectory=$env:SYSTEMDRIVE + "\Artifacts\" + $serviceName + "\release-" + $releaseId

# Check if artifact is File or Folder structure
$isFile = Test-Path -Path $artifactFilename -PathType Leaf
$isFolder = Test-Path -Path $artifactFilename -PathType Container

if ($isFile -and ([IO.Path]::GetExtension($artifactFilename) -eq "*.zip" ))
{
    Write-Host "Extracting package from" $artifactFilename "to" $appPhysicalDirectory
    Expand-Archive -Path $artifactFilename -DestinationPath $appPhysicalDirectory -Force
}
else
{
    Write-Host "Artifact type is not a zip file. Skipping extract."
}
if ($isFolder)
{
    Write-Host "Copying folder from" $artifactFilename "to" $appPhysicalDirectory
    Copy-Item -Path $artifactFilename -Filter "*.*" -Recurse -Destination $appPhysicalDirectory -Container
}
Write-Host "Done."
