set -x

if [ -z "${HARNESS_TEAM}" ]
then
  if [ ! -z "${ghprbPullTitle}" ]
  then
    HARNESS_TEAM=`echo "${ghprbPullTitle}" | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  else
    HARNESS_TEAM=`git log -1 --pretty=format:%s | sed -e 's/[[]\([A-Z]*\)-[0-9]*]:.*/\1/g'`
  fi
fi

if [ -z "${HARNESS_TEAM}" ]
then
  echo failed to deduct the team this commit is for
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

#bazel-bin/tools/rust/aeriform/aeriform analyze --team-filter $TEAM

if [ ! -z "$TRACK_FILES" ]
then
	scripts/bazel/aeriform.sh analyze \
    ${TRACK_FILES} \
    --kind-filter AutoAction \
    --kind-filter Critical \
    --kind-filter ToDo \
    --kind-filter Warning \
    --exit-code
fi
