$user = '<user>'
$pass = '<password>'
$srcPath = 'https://harness.jfrog.io/harness/generic-repo/todolist/todolist.war'
$destFile = $env:TEMP+'\artifactory.zip'
$pair = "$($user):$($pass)"
$encodedCreds = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($pair))
$basicAuthValue = "Basic $encodedCreds"
$Headers = @{
    Authorization = $basicAuthValue
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $srcPath -Headers $Headers -OutFile $destFile
