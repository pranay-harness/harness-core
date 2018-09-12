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