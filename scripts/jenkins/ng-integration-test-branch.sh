#!/bin/bash

if [ "${PURPOSE}" = "ng-integration-test" ]
then
    echo "Valid purpose ${PURPOSE}"
else
   echo "Unknown purpose ${PURPOSE}"
   exit 1
fi

# initialize variables
export VERSION_FILE=build.properties

export VERSION=`cat ${VERSION_FILE} |\
    grep 'build.number=' |\
    sed -e 's: *build.number=::g'`

export BRANCH=`echo "${GIT_BRANCH}" | sed -e "s/origin\///g"`
export SHA=`git rev-parse HEAD`

# Prepare new commit
git checkout ${BRANCH}

# Export variables
echo VERSION=${VERSION} > jenkins.properties
echo BRANCH=${BRANCH} >> jenkins.properties
echo SHA=${SHA} >> jenkins.properties