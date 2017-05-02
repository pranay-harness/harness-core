if [ $# -ne 3 ]
then
  echo "This script is used to set version on delegates."
  echo "Usage: $0 <version> <buildname> <environment> <logdnakey>"
  echo "buildname: jenkins build name"
  echo "environment: demo, ci"
  echo "logdnakey: key for logging against right logdna instance."
  exit 1
fi

echo "System-Properties: version=1.0.${1} logdnakey=${4}" >> app.mf
echo "Application-Version: version=1.0.${1}" >> app.mf
jar ufm delegate-0.0.1-SNAPSHOT-capsule.jar app.mf
rm -rf app.mf
echo "1.0.${1} jobs/${2}/${1}/delegate.jar" >> delegate${3}.txt
mv delegate-0.0.1-SNAPSHOT-capsule.jar delegate.jar
