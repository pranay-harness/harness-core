if [ $# -ne 3 ]
then
  echo "This script is used to set version on watchers."
  echo "Usage: $0 <version> <environment> <logdnakey>"
  echo "version: portal build number"
  echo "environment: ci, qa, prod"
  echo "logdnakey: key for logging against right logdna instance."
  exit 1
fi

echo "System-Properties: version=1.0.${1} logdnakey=${3}" >> app.mf
echo "Application-Version: version=1.0.${1}" >> app.mf
jar ufm watcher-capsule.jar app.mf
rm -rf app.mf
echo "1.0.${1} latest/watcher.jar" >> watcher${2}.txt
mv watcher-capsule.jar watcher.jar
