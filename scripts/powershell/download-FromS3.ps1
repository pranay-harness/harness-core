# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

$accessKey = '<accessKey>'
$secretKey = '<secretKey>'
$bucketName = '<bucket>'
$key = '<key>'
$localfile = 'local-sample.txt'

if (Get-Module -ListAvailable -Name AWSPowerShell) {
    Write-Host "AWSPowerShell Module exists"

} else {
    Write-Host "Module does not exist, Installing"
    Install-Module -Name AWSPowerShell -SkipPublisherCheck -Force
}

Import-Module AWSPowerShell

Read-S3Object -BucketName $bucketName -Key $key -File $localfile -AccessKey $accessKey -SecretKey $secretKey
