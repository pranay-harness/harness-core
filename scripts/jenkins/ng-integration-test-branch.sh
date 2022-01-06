#!/bin/bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
