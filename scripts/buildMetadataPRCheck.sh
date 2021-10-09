set -x
export ghprbTargetBranch=<+pipeline.variables.CITargetBranch>
export ghprbSourceBranch=<+pipeline.variables.CISourceBranch>
export BRANCH_PREFIX=`echo ${ghprbTargetBranch} | sed 's/\(........\).*/\1/g'`
export VERSION_FILE=build.properties

export VERSION=`cat ${VERSION_FILE} |\
grep 'build.number=' |\
sed -e 's: *build.number=::g'`
export OLD_VERSION=$(( ${VERSION}-1 ))

export PATCH=`cat ${VERSION_FILE} |\
grep 'build.patch=' |\
sed -e 's: *build.patch=::g'`

git checkout origin/${ghprbTargetBranch} -b temp246
git checkout ${ghprbTargetBranch}

set -e

if [ "${BRANCH_PREFIX}" = "release/" ] && [ `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.number=$VERSION"` ] && [ `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.patch=$PATCH"` ]
then
    echo "Both buildversion and patch cannot be modified"
    exit 1
elif [ "${BRANCH_PREFIX}" = "release/" ] && [ ! `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.number=$VERSION"` ] && [ ! `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.patch=$PATCH"` ]
then
    echo "Either buildversion or patch version must be incremented"
    exit 1
elif [ "${BRANCH_PREFIX}" = "release/" ] && [ `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.number=$VERSION"` ]
then
    git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "\-build.number=$OLD_VERSION"
elif [ "${BRANCH_PREFIX}" = "release/" ] && [ `git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.patch=$PATCH"` ]
then
    export OLD_PATCH=$(printf %03d $(( ${PATCH}-1 )) )
    git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "+build.patch=$PATCH"
    git diff temp246..${ghprbTargetBranch} -- ${VERSION_FILE} | grep "\-build.patch=$OLD_PATCH"
fi


if [ "${ghprbTargetBranch}" = "master" ]
then
    git show ${VERSION_FILE} | grep "build.number" || exit 0 && exit 1
    git show ${VERSION_FILE} | grep "build.patch" || exit 0 && exit 1
fi