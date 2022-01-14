#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`cat "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo ${PROP_VALUE}
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

# Read value from values.internal.yaml
function rv() {
    KEY=$1
    echo $(yq r values.internal.yaml ${KEY})
}

# Write value to values.internal.yaml
function wv() {
    KEY=$1
    VALUE=$2
    yq w -i values.internal.yaml "${KEY}" "${VALUE}"
}
