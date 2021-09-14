#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


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

if [[ $UNMERGED_SINCE -lt $(($NOW - 86400)) ]]
then
  echo It is too old, please merge your branch with ${ghprbTargetBranch}
  exit 1
fi
