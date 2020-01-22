#!/bin/bash

# Check for not merged hot fixes
git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE '\[(CCM|CCE|CD|CDP|CDC|DX|CE|CV|DOC|ER|HAR|LE|PL|SEC|SWAT)-[0-9]+]:' -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE '\[(CCM|CCE|CD|CDP|CDC|DX|CE|CV|DOC|ER|HAR|LE|PL|SEC|SWAT)-[0-9]+]:' -o | sort | uniq > master.txt

NOT_MERGED=`comm -23 release.txt master.txt`

if [ ! -z "$NOT_MERGED" ]
then
    echo "There are jira issues in release branches that are not reflected in master."
    exit 1
fi


# initialize variables
export VERSION_FILE=build.properties

export VERSION=`cat ${VERSION_FILE} |\
    grep 'build.number=' |\
    sed -e 's: *build.number=::g'`
export VERSION=${VERSION%??}
export NEW_VERSION=$(( ${VERSION}+1 ))

export BRANCH=`echo "${GIT_BRANCH}" | sed -e "s/origin\///g"`
export SHA=`git rev-parse HEAD`

# Update jira issues
scripts/jenkins/release-branch-update-jiras.sh

# Prepare new release commit
git checkout ${BRANCH}

sed -i "s:build.number=${VERSION}00:build.number=${NEW_VERSION}00:g" ${VERSION_FILE}
git add ${VERSION_FILE}
git commit -m "Branching to release/${PURPOSE}/${VERSION}xx. New version ${NEW_VERSION}xx"


# Export variables
echo VERSION=${VERSION} > jenkins.properties
echo BRANCH=${BRANCH} >> jenkins.properties
echo SHA=${SHA} >> jenkins.properties