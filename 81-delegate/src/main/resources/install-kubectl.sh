#!/usr/bin/env bash

downloadDirectory=$1
downloadUrl=$2

cd $downloadDirectory
curl -LO $downloadUrl
chmod +x ./kubectl

echo Checking kubectl version
./kubectl version --short --client
