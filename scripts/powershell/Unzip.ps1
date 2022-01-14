$pathToZip = 'artifact.zip'
$targetDir = $env:Temp
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
[System.IO.Compression.ZipFile]::ExtractToDirectory($pathToZip, $targetDir)