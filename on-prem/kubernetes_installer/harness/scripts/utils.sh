#!/usr/bin/env bash

set -e

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

function saveImage(){
    imageName=$1
    imageLocation=$2

    docker pull $imageName && docker save $imageName > $imageLocation
}

function replace() {
        if [[ "$OSTYPE" == "darwin"* ]]; then
                find $3 -exec sed -i '' -e "s|$1|$2|g" {} +
        else
                find $3 -exec sed -i "s|$1|$2|g" {} +
        fi
}

function confirm() {
    while true; do
        read -p "Do you wish to continue? [y/n]: " yn
        case $yn in
            [Yy]* ) break;;
            [Nn]* ) exit;;
            * ) echo "Please answer y or n.";;
        esac
    done
}