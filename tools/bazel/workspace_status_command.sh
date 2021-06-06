#!/usr/bin/env bash

set -e

case ${BUILD_PURPOSE} in
    DEVELOPMENT|PR_CHECK|FEATURE|RELEASE)  ;;
    *) exit 1 ;;
esac

echo "TIMESTAMP $(date +'%y%m%d-%H%M')"

if [ "${BUILD_PURPOSE}" = "DEVELOPMENT" -o "${BUILD_PURPOSE}" = "PR_CHECK" ]
then
  echo "STABLE_GIT_BRANCH unknown-development-branch"
  echo "STABLE_GIT_COMMIT 0000000000000000000000000000000000000000"

  echo STABLE_MAJOR_VERSION 1
  echo STABLE_MINOR_VERSION 0
  echo STABLE_BUILD_NUMBER 0
  echo STABLE_PATCH dev
else
  echo "STABLE_GIT_BRANCH $(git rev-parse --abbrev-ref HEAD)"
  echo "STABLE_GIT_COMMIT $(git rev-parse HEAD)"

  echo STABLE_MAJOR_VERSION $(getProperty ${PROPERTIES} "build.majorVersion")
  echo STABLE_MINOR_VERSION $(getProperty ${PROPERTIES} "build.minorVersion")
  echo STABLE_BUILD_NUMBER $(getProperty ${PROPERTIES} "build.number")
  echo STABLE_PATCH $(getProperty ${PROPERTIES} "build.patch")
fi

function getProperty () {
   cat "$1" | grep "^$2=" | cut -d"=" -f2
}

PROPERTIES=build.properties