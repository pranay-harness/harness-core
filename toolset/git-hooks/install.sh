#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

pushd `dirname $0` > /dev/null && BASEDIR=$(pwd -L) && popd > /dev/null

echo This script will install hooks that run scripts that could be updated without notice.

while true; do
    read -p "Do you wish to install these hooks?" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

cd $BASEDIR
find . -type f -not -name "*.sh" -exec cp "{}" ../../.git/hooks \;
