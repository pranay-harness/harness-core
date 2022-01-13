#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPA|OPS|PIE|PL|SEC|SWAT|GTM|ONP"

# Check for not merged hot fixes
git log --remotes=origin/release/* --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq > release.txt
git log --remotes=origin/[m]aster --pretty=oneline --abbrev-commit | grep -iE "\[(${PROJECTS})-[0-9]+]:" -o | sort | uniq > master.txt

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
DV=`cat ${VERSION_FILE} | grep 'delegate.version=' | sed -e 's: *delegate.version=::g'`
YEAR=$(date +%y)
MONTH=$(date +%m)
yy="$(echo "$DV" | cut -d'.' -f1)"
mm="$(echo "$DV" | cut -d'.' -f2)"
mv="$(echo "$DV" | cut -d'.' -f3)"
if [ "$MONTH" -gt "$mm" ] || [ "$YEAR" -gt "$yy" ]; then NEWDELEGATEVERSION="10"; else NEWDELEGATEVERSION=$((${mv}+1)); fi
export NEWDELEGATEVERSION
export VERSION=${VERSION%??}
export NEW_VERSION=$(( ${VERSION}+1 ))

export BRANCH=`echo "${GIT_BRANCH}" | sed -e "s/origin\///g"`
export SHA=`git rev-parse HEAD`

# Update jira issues
scripts/jenkins/release-branch-update-jiras.sh
scripts/jenkins/release-branch-update-jira_status.sh

# Prepare new release commit
git checkout ${BRANCH}

sed -i "s:build.number=${VERSION}00:build.number=${NEW_VERSION}00:g" ${VERSION_FILE}
sed -i "s#${DV}#${YEAR}.${MONTH}.${NEWDELEGATEVERSION}#g" ${VERSION_FILE}
git add ${VERSION_FILE}
git commit -m "Branching to release/${PURPOSE}/${VERSION}xx. New version ${NEW_VERSION}xx"


# Export variables
echo VERSION=${VERSION} > jenkins.properties
echo BRANCH=${BRANCH} >> jenkins.properties
echo SHA=${SHA} >> jenkins.properties
