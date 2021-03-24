#!/bin/bash

LAST_UNMERGED_SHA=`git rev-list --left-right origin/${ghprbTargetBranch}...HEAD | tail -n 1 | cut -c 2-`

if [ -z "${LAST_UNMERGED_SHA}" ]
then
  exit 0
fi

UNMERGED_SINCE=`git show -s --format=%ct ${LAST_UNMERGED_SHA}`
NOW=`date +'%s'`

if [[ $UNMERGED_SINCE -lt $(($NOW - 86400)) ]]
then
  echo There are too old commit that are not merged to your branch.
  exit 1
fi