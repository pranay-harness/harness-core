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

Invoke-WebRequest -Uri $srcPath -Headers $Headers -OutFile $destFile