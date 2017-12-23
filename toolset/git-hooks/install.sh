#!/bin/sh
pushd `dirname $0` > /dev/null && BASEDIR=$(pwd -L) && popd > /dev/null

pwd

cd $BASEDIR
find . -type f -not -name "*.sh" -exec cp "{}" ../../.git/hooks \;