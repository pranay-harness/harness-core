set -x

if [ "${PLATFORM}" != "jenkins" ]
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

sed -i.bak "s|\${build.number}|${buildNo}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitCommit}|${GIT_COMMIT}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitBranch}|${GIT_BRANCH}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${timestamp}|${timestamp}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"

sed -i.bak "s|\${build.fullVersion}|${buildMajorVersion}.${buildMinorVersion}.${buildNo}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
