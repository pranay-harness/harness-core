if [ $# -ne 4 ]
then
  echo "This script is used to set version on watchers."
  echo "Usage: $0 <version> <buildname> <environment> <logdnakey>"
  echo "buildname: jenkins build name"
  echo "environment: ci, qa, prod"
  echo "logdnakey: key for logging against right logdna instance."
  exit 1
fi

echo "System-Properties: version=1.0.${1} logdnakey=${4}" >> app.mf
echo "Application-Version: version=1.0.${1}" >> app.mf
jar ufm watcher-capsule.jar app.mf
rm -rf app.mf
echo "1.0.${1} jobs/${2}/${1}/watcher.jar" >> watcher${3}.txt
mv watcher-capsule.jar watcher.jar
