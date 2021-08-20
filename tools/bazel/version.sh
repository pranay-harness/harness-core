#!/usr/bin/env bash

set -e

STABLE_PROPERTIES=$1
VOLATILE_PROPERTIES=$2

function getProperty () {
   VALUE=`cat "${STABLE_PROPERTIES}" | grep "^STABLE_$1 " | cut -d" " -f2`
   if [ ! -z "$VALUE" ]; then
     echo $VALUE
   else
     cat "${VOLATILE_PROPERTIES}" | grep "^$1 " | cut -d" " -f2
   fi
}

MAJOR=$(getProperty "MAJOR_VERSION")
MINOR=$(getProperty "MINOR_VERSION")
NUMBER=$(getProperty "BUILD_NUMBER")
PATCH=$(getProperty "PATCH")
echo "version   : ${MAJOR}.${MINOR}.${NUMBER}"
echo "buildNo   : ${NUMBER}"
echo "patch     : ${PATCH}"

TIMESTAMP=$(getProperty "TIMESTAMP")
echo "timestamp : ${TIMESTAMP}"

GIT_COMMIT=$(getProperty "GIT_COMMIT")
GIT_BRANCH=$(getProperty "GIT_BRANCH")
echo "gitCommit : ${GIT_COMMIT}"
echo "gitBranch : ${GIT_BRANCH}"
