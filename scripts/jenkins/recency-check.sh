#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [ -z "${ghprbTargetBranch}" ]
then
  ghprbTargetBranch=master
fi

BREAKING_COMMITS=`git log --pretty=oneline --no-merges HEAD..origin/${ghprbTargetBranch} | grep '\[PR_FIX\]\|\[REFACTORING\]'`

if [ ! -z "${BREAKING_COMMITS}" ]
then
  echo There are breaking commits that you need to merge.
  echo "${BREAKING_COMMITS}"
  exit 1
fi

LAST_UNMERGED_SHA=`git log --format=format:%H --no-merges HEAD..origin/${ghprbTargetBranch} | tail -n 1`
if [ -z "${LAST_UNMERGED_SHA}" ]
then
  exit 0
fi

echo last unmerged commit
git log -n 1 ${LAST_UNMERGED_SHA}

UNMERGED_SINCE=`git show -s --format=%ct ${LAST_UNMERGED_SHA}`
NOW=`date +'%s'`
RECENCY_WINDOW=$((3*24*60*60))

if [[ $UNMERGED_SINCE -lt $(($NOW - $RECENCY_WINDOW)) ]]
then
  echo It is too old, please merge your branch with ${ghprbTargetBranch}
  exit 1
fi
