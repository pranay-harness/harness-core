#!/usr/bin/env bash

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`cat "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}

rm -f harness_installer.zip

mkdir -p harness_installer
cp scripts/first_time_only_install_harness.sh harness_installer
cp scripts/upgrade_harness.sh harness_installer

cp README.txt harness_installer
mongo_version=$(getProperty "version.properties" "mongo")
manager_version=$(getProperty "version.properties" "manager")
delegate_version=$(getProperty "version.properties" "delegate")
watcher_version=$(getProperty "version.properties" "watcher")
proxy_version=$(getProperty "version.properties" "proxy")
ui_version=$(getProperty "version.properties" "ui")
learning_engine_version=$(getProperty "version.properties" "learning-engine")

echo "Manager version is ${manager_version}"
echo "Mongo version is ${mongo_version}"
echo "Delegate version is ${delegate_version}"
echo "Watcher version is ${watcher_version}"
echo "Proxy version is ${proxy_version}"
echo "UI version is ${ui_version}"
echo "Learning Engine version is ${learning_engine_version}"

if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -e "s|MONGO_VERSION|${mongo_version}|g" harness_installer/first_time_only_install_harness.sh
else
    sed -i "s|MONGO_VERSION|${mongo_version}|g" harness_installer/first_time_only_install_harness.sh
fi

cp -r ../harness_disconnected_on_prem_pov_final harness_installer/
mkdir -p harness_installer/harness_disconnected_on_prem_pov_final/images
cp version.properties harness_installer/harness_disconnected_on_prem_pov_final/

docker pull harness/manager:${manager_version}
docker pull harness/learning-engine:${learning_engine_version}
docker pull harness/ui:${ui_version}
docker pull harness/proxy:${proxy_version}

docker save harness/manager:${manager_version} > harness_installer/harness_disconnected_on_prem_pov_final/images/manager.tar
docker save harness/learning-engine:${learning_engine_version} > harness_installer/harness_disconnected_on_prem_pov_final/images/learning_engine.tar
docker save harness/ui:${ui_version} > harness_installer/harness_disconnected_on_prem_pov_final/images/ui.tar
docker save harness/proxy:${proxy_version} > harness_installer/harness_disconnected_on_prem_pov_final/images/proxy.tar

curl https://app.harness.io/storage/wingsdelegates/jre/8u131/jre-8u131-solaris-x64.tar.gz > jre-8u131-solaris-x64.tar.gz

curl https://app.harness.io/storage/wingsdelegates/jre/8u131/jre-8u131-macosx-x64.tar.gz > jre-8u131-macosx-x64.tar.gz

curl https://app.harness.io/storage/wingsdelegates/jre/8u131/jre-8u131-linux-x64.tar.gz > jre-8u131-linux-x64.tar.gz

mv delegate.jar harness_installer/harness_disconnected_on_prem_pov_final/images/
mv watcher.jar harness_installer/harness_disconnected_on_prem_pov_final/images/
mv jre-8u131-solaris-x64.tar.gz harness_installer/harness_disconnected_on_prem_pov_final/images/
mv jre-8u131-macosx-x64.tar.gz harness_installer/harness_disconnected_on_prem_pov_final/images/
mv jre-8u131-linux-x64.tar.gz harness_installer/harness_disconnected_on_prem_pov_final/images/

zip -r -X harness_installer.zip harness_installer
rm -rf harness_installer