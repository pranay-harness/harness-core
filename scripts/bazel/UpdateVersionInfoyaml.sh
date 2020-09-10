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

buildNo=$(getProperty "build.properties" "build.number")
buildMajorVersion=$(getProperty "build.properties" "build.majorVersion")
buildMinorVersion=$(getProperty "build.properties" "build.minorVersion")

sed -i.bak "s|\${build.number}|${buildNo}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitCommit}|${gitCommit}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${gitBranch}|${gitBranch}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"
sed -i.bak "s|\${timestamp}|${timestamp}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"

sed -i.bak "s|\${build.fullVersion}|${buildMajorVersion}.${buildMinorVersion}.${buildNo}|g"  "12-commons/src/main/resources-filtered/versionInfo.yaml"

