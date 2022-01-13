#!/usr/bin/env bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`cat "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}

function generateRandomString(){
   echo `hexdump -n 16 -e '4/4 "%08X" 1 "\n"' /dev/urandom`
}

function generateRandomStringOfLength(){
    LENGTH=$1
    echo `cat /dev/urandom | LC_CTYPE=C tr -dc "[:alnum:]" | head -c $LENGTH`
}

function replace() {
        if [[ "$OSTYPE" == "darwin"* ]]; then
                find config -type f -name "*.*" -exec sed -i '' -e "s|$1|$2|g" {} +
        else
                find config -type f -name "*.*" -exec sed -i "s|$1|$2|g" {} +
        fi
}

function stopDockerContainer(){
    CONTAINERNAME=$1
    if [[ $(checkDockerImageRunning "$CONTAINERNAME") -eq 0 ]]; then
        echo "Killing container "$CONTAINERNAME
        docker kill $CONTAINERNAME
    fi
}

function stopContainers(){
   echo "################################Stopping services ################################"

    stopDockerContainer "mongoContainer"
    stopDockerContainer "verificationService"
    stopDockerContainer "harness-proxy"
    stopDockerContainer "harnessManager"
    stopDockerContainer "harness_ui"
    stopDockerContainer "learningEngine"
    stopDockerContainer "harness-timescaledb"
}

function checkDockerImageRunning(){
    name=$1
    if [ "$(docker ps -q -f name=$name)" ]; then
        echo 0
    else
        echo 1
    fi
}

function checkIfDirectoryExists(){
    dirName=$1;
    if [[ -d $dirName ]]; then
        echo 0;
    else
        echo 1
    fi
}

function checkIfFileExists(){
    fileName=$1;
    if [[ -e $fileName ]]; then
        echo 0;
    else
        echo 1
    fi
}

function checkMongoUpgrade(){

        echo "checking if upgrade is possible..."

        if [[ $(checkDockerImageRunning "mongoContainer") -eq 1 ]]; then
                echo "Existing installation found but Mongo is not running. Please start harness using previous install bundle before retrying the upgrade."
                exit 1
        fi

        INCOMING_MONGO=`echo $MONGO_VERSION | cut -c1-3`
        RUNNING_MONGO=`docker exec mongoContainer mongo --port 7144 --quiet  --eval 'db.version()' |cut -c1-3`

        echo Current mongo version is : $RUNNING_MONGO
        echo Mongo version from this install bundle is : $INCOMING_MONGO

        if [[ $RUNNING_MONGO != $INCOMING_MONGO ]]; then
                if [[ $INCOMING_MONGO == "3.6" ]]; then
                        echo "Mongo is running at higher version. Please use install bundle version 62704 or higher"
                        exit 1
                elif [[ $INCOMING_MONGO == "4.0" ]]; then
                        if [[ $RUNNING_MONGO == "3.6" ]]; then
                                echo "Running auth schema upgrade..."
                                docker exec mongoContainer mongo "mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port/?authSource=admin" --quiet --eval 'db.adminCommand({authSchemaUpgrade: 1});'
                         elif [[ $RUNNING_MONGO == "4.2" ]]; then
                                echo "Cannot downgrade mongo. Please use higher version of install bundle"
                                exit 1
                        fi
                elif [[ $INCOMING_MONGO == "4.2" ]]; then
                        if [[ $RUNNING_MONGO == "4.0" ]]; then
                                echo "Setting Feature Compatibility Version to 4.0.."
                                docker exec mongoContainer mongo "mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port/?authSource=admin" --quiet --eval 'db.adminCommand({ setFeatureCompatibilityVersion: "4.0" });'
                                upgrade42=true
                        elif [[ $RUNNING_MONGO == "3.6" ]]; then
                                echo "Cannot upgrade. Please use version 62704 before upgrading to this version"
                                exit 1
                        fi
                fi
        fi
}

function setCompatibility42() {
  echo "Setting Feature Compatibility Version to 4.2.."
  docker exec mongoContainer mongo "mongodb://$mongodbUserName:$mongodbPassword@$host1:$mongodb_port/?authSource=admin" --quiet --eval 'db.adminCommand({ setFeatureCompatibilityVersion: "4.2" });'
}
