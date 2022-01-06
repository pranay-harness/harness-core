# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

$releaseId="${workflow.ReleaseNo}"
$serviceName="${serviceName}"
$artifactFilename = $env:TEMP + "\" + $serviceName + "-release-" + $releaseId + ".zip"

Write-Host "Starting Deployment [id:" $releaseId "]"
Write-Host "Downloading artifact file - URL: ${artifact.url} to File: " $artifactFilename
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri "${artifact.url}" -OutFile $artifactFilename
Write-Host "Done."
