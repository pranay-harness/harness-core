#!/usr/bin/env bash
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