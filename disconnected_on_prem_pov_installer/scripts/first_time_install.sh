#!/usr/bin/env bash
echo "This script will prepare the on_prem_pov with mongodb docker image"
echo "Pulling mongo:MONGO_VERSION"
docker pull mongo:MONGO_VERSION

docker inspect mongo:MONGO_VERSION

if [[ !($? -eq 0) ]];then
    echo "Mongo docker image has not been downloaded, check connectivity to DockerHub"
    exit 1
fi

echo "Saving mongo to a tar"
docker save mongo:MONGO_VERSION > harness_disconnected_on_prem_pov_final/images/mongo.tar

zip -r -X harness_disconnected_on_prem_pov_final.zip harness_disconnected_on_prem_pov_final

echo "Final zip file has been created for the first time install, scp the zip file to the remote machine"