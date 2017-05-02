if [ $# -ne 3 ]
then
  echo "This script is used to set version on delegates."
  echo "Usage: $0 <version>"
  exit 1
fi

echo "System-Properties: version=1.0.${1}" >> app.mf
echo "Application-Version: version=1.0.${1}" >> app.mf
jar ufm delegate-0.0.1-SNAPSHOT-capsule.jar app.mf
rm -rf app.mf
