set -e

if [ -z "${HARNESS_TEAM}" ]
then
  if [ ! -z "${ghprbPullTitle}" ]
  then
    PROJECT=`echo "${ghprbPullTitle}" | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  else
    PROJECT=`git log -1 --pretty=format:%s | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  fi

  case "${PROJECT}" in
     "BT") export HARNESS_TEAM="PL"
     ;;
     "CCE") export HARNESS_TEAM="CE"
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
  esac
fi

if [ -z "${HARNESS_TEAM}" ]
then
  echo failed to deduct the team this commit is for, the project is "${PROJECT}"
  exit 1
fi

if [ -z "${ghprbTargetBranch}" ]
then
  if which hub > /dev/null
  then
    ghprbTargetBranch=`hub pr show --format=%B`
  fi
fi

if [ -z "${ghprbTargetBranch}" ]
then
  ghprbTargetBranch=`git rev-parse --abbrev-ref HEAD | sed -e "s/^\([^@]*\)$/\1@master/" | sed -e "s/^.*@//"`
fi

BASE_SHA=`git merge-base origin/${ghprbTargetBranch} HEAD`
TRACK_FILES=`git diff --diff-filter=ACM --name-status ${BASE_SHA}..HEAD | grep ".java$" | awk '{ print "--location-class-filter "$2}' | tr '\n' ' '`

scripts/bazel/prepare_aeriform.sh

scripts/bazel/aeriform.sh analyze \
  --kind-filter Critical \
  --exit-code

if [ ! -z "$TRACK_FILES" ]
then
	scripts/bazel/aeriform.sh analyze \
    ${TRACK_FILES} \
    --kind-filter AutoAction \
    --kind-filter Critical \
    --kind-filter ToDo \
    --kind-filter Warning \
    --exit-code

	scripts/bazel/aeriform.sh analyze \
    ${TRACK_FILES} \
    --team-filter ${HARNESS_TEAM} \
    --exit-code
fi
