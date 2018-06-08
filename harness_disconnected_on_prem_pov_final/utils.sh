#!/usr/bin/env bash

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
        sudo docker kill $CONTAINERNAME
    fi

}

function stopContainers(){
   echo "################################Stopping services ################################"

    stopDockerContainer "mongoContainer"
    stopDockerContainer "harness-proxy"
    stopDockerContainer "harnessManager"
    stopDockerContainer "harness_ui"
    stopDockerContainer "learningEngine"
}

function checkDockerImageRunning(){
    name=$1
    #echo "name is "$name
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


