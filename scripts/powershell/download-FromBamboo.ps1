# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

$user = '<username>'
$pass = '<password>'
$srcPath = 'http://bamboo-server/browse/TOD-TOD-193/artifact/JOB1/artifacts/todolist.war'
$destFile = $env:TEMP+'\artifact.zip'
$pair = "$($user):$($pass)"
$encodedCreds = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($pair))
$basicAuthValue = "Basic $encodedCreds"
$Headers = @{
    Authorization = $basicAuthValue
}
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri $srcPath -Headers $Headers -OutFile $destFile
