set -x

if [[ "$BUILD_TAG" == *pr-portal-* || "${PLATFORM}" != jenkins ]]
then
  exit 0
fi

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`head -3 "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}

buildNo=$1
buildMajorVersion=$(getProperty "build.properties" "build.majorVersion")
buildMinorVersion=$(getProperty "build.properties" "build.minorVersion")
timestamp=$( date +'%y%m%d-%H%M')

sed -i.bak "s|\${build.number}|${buildNo}|g"  "980-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitCommitId}|${GIT_COMMIT}|g"  "980-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitBranch}|${GIT_BRANCH}|g"  "980-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${buildTimeStamp}|${timestamp}|g"  "980-commons/src/main/resources-filtered/versionInfo.yaml"

sed -i.bak "s|\${build.fullVersion}|${buildMajorVersion}.${buildMinorVersion}.${buildNo}|g"  "980-commons/src/main/resources-filtered/versionInfo.yaml"
