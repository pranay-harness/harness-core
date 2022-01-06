# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

if [ -z "${HARNESS_TEAM}" ]
then
  if [ ! -z "${ghprbPullTitle}" ]
  then
    PROJECT=`echo "${ghprbPullTitle}" | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  else
    COMMIT_MSG=`git log -1 --pretty=format:%s`
    if [[ $input = "Merge commit*" ]]
    then
      COMMIT_MSG=`git log -2 --pretty=format:%s | tail -1`
    fi
    PROJECT=`git log -1 --pretty=format:%s | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  fi

  case "${PROJECT}" in
     "BT") export HARNESS_TEAM="PL"
     ;;
     "CCE") export HARNESS_TEAM="CE"
     ;;
     "PIE") export HARNESS_TEAM="PIPELINE"
     ;;
     "CCM") export HARNESS_TEAM="CE"
     ;;
     "CDC") export HARNESS_TEAM="CDC"
     ;;
     "CDNG") export HARNESS_TEAM="CDC"
     ;;
     "CDP") export HARNESS_TEAM="CDP"
     ;;
     "CE") export HARNESS_TEAM="CE"
     ;;
     "CI") export HARNESS_TEAM="CI"
     ;;
     "CV") export HARNESS_TEAM="CV"
     ;;
     "CVNG") export HARNESS_TEAM="CV"
     ;;
     "DEL") export HARNESS_TEAM="DEL"
     ;;
     "DOC") export HARNESS_TEAM="DX"
     ;;
     "DX") export HARNESS_TEAM="DX"
     ;;
     "ER") export HARNESS_TEAM="CDC"
     ;;
     "DX") export HARNESS_TEAM="DX"
     ;;
     "FFM") HARNESS_TEAM="CF"
     ;;
     "OPS") export HARNESS_TEAM="PL"
     ;;
     "PL") export HARNESS_TEAM="PL"
     ;;
     "SEC") export HARNESS_TEAM="PL"
     ;;
     "SWAT") export HARNESS_TEAM="PL"
     ;;
     "GTM") export HARNESS_TEAM="GTM"
     ;;
     "ONP") export HARNESS_TEAM="PL"
     ;;
     "GIT") export HARNESS_TEAM="CDP"
     ;;
     "LWG") export HARNESS_TEAM="LWG"
     ;;
  esac
fi

if [ -z "${HARNESS_TEAM}" ]
then
  echo failed to deduct the team this commit is for, the project is "${PROJECT}"
  exit 1
fi

if [ -z "${ghprbTargetBranch}" ]
then
  if which hub &>/dev/null
  then
    ghprbTargetBranch=`hub pr show --format=%B` || true
  fi
fi

if [ -z "${ghprbTargetBranch}" ]
then
  ghprbTargetBranch=`git rev-parse --abbrev-ref HEAD | sed -e "s/^\([^@]*\)$/\1@master/" | sed -e "s/^.*@//"`
fi

BASE_SHA=`git merge-base origin/${ghprbTargetBranch} HEAD`

FIXES=$(git diff ${BASE_SHA}..HEAD | grep '+@BreakDependencyOn\|+@TargetModule' | wc -l)

TMP_FILE=$(mktemp)

git diff --diff-filter=ACM --name-status ${BASE_SHA}..HEAD | grep ".java$" | awk '{ print $2}' > $TMP_FILE
TRACK_FILES=`while read file; do echo $(git log --pretty=format:%ad -n 1 --date=format:'%Y%m%d%H%M%S' -- $file) $file; done < $TMP_FILE | sort | head -n 5 | awk '{ print "--location-class-filter "$2}'`
TRACK_MODULES=`cat $TMP_FILE | awk -F / '{ print $1 }' | sort | uniq | awk '{ print "--root-filter //" $1 }'`

scripts/bazel/prepare_aeriform.sh

scripts/bazel/aeriform.sh analyze \
  --kind-filter Critical \
  --top-blockers=25 \
  --exit-code

if [ $FIXES -gt 9 ]
then
  echo "$FIXES is enough for one PR"
  exit 0
fi

if [ ! -z "$TRACK_FILES" ]
then
	scripts/bazel/aeriform.sh analyze \
    ${TRACK_FILES} \
    --kind-filter ToDo \
    --exit-code

	scripts/bazel/aeriform.sh analyze \
    ${TRACK_FILES} \
    --team-filter ${HARNESS_TEAM} \
    --kind-filter AutoAction \
    --kind-filter Error \
    --kind-filter Warning \
    --exit-code
fi

if [ ! -z "$TRACK_MODULES" ]
then
  RENAMED_FILES=$(git diff --diff-filter=R --name-status ${BASE_SHA}..HEAD | wc -l)

  if [ $RENAMED_FILES -eq 0 ]
  then
    OWNED_BY_FIXES=$(git diff ${BASE_SHA}..HEAD | grep '+@OwnedBy' | wc -l)
    if [ $OWNED_BY_FIXES -lt 5 ]
    then
      scripts/bazel/aeriform.sh analyze \
        ${TRACK_MODULES} \
        --team-filter ${HARNESS_TEAM} \
        --kind-filter AutoAction \
        --auto-actionable-command \
        --exit-code
    fi
  fi
fi
