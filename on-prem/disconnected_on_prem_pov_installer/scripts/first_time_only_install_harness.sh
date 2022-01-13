#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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

tar -cvzf harness_disconnected_on_prem_pov_final.tar.gz harness_disconnected_on_prem_pov_final

echo "Final tar.gz file has been created for the first time install, scp the tar.gz file to the remote machine"
