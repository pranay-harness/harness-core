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
                find $4 -type f -name "$3" -exec sed -i '' -e "s|$1|$2|g" {} +
        else
                find $4 -type f -name "$3" -exec sed -i "s|$1|$2|g" {} +
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