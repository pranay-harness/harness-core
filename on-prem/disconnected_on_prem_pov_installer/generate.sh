#!/usr/bin/env bash
set -e

apt-get update
apt-get install -y curl
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
rm get-docker.sh

INSTALLER_DIR=harness_installer
INSTALLER_COMPRESSED_FILE=harness_installer.tar.gz
INSTALLER_TEMPLATE_DIR=harness_disconnected_on_prem_pov_final
SCRIPTS_DIR=scripts
CONFIG_PROPERTIES_FILE="${INSTALLER_DIR}/${INSTALLER_TEMPLATE_DIR}/config.properties"
FIRST_TIME_INSTALL_SCRIPT_FILE=first_time_only_install_harness.sh
UPGRADE_SCRIPT_FILE=upgrade_harness.sh
VERSION_PROPERTIES_FILE=version.properties

IMAGES_DIR="${INSTALLER_DIR}/${INSTALLER_TEMPLATE_DIR}/images"
MANAGER_IMAGE="harness/manager:${MANAGER_VERSION}"
LEARNING_ENGINE_IMAGE="harness/learning-engine:${LEARNING_ENGINE_VERSION}"
UI_IMAGE="harness/ui:${UI_VERSION}"
PROXY_IMAGE="harness/proxy:${PROXY_VERSION}"

MANAGER_IMAGE_TAR="${IMAGES_DIR}/manager.tar"
LEARNING_ENGINE_IMAGE_TAR="${IMAGES_DIR}/learning_engine.tar"
UI_IMAGE_TAR="${IMAGES_DIR}/ui.tar"
PROXY_IMAGE_TAR="${IMAGES_DIR}/proxy.tar"

JRE_SOURCE_URL=https://app.harness.io/storage/wingsdelegates/jre/8u131
JRE_SOLARIS=jre-8u131-solaris-x64.tar.gz
JRE_MACOSX=jre-8u131-macosx-x64.tar.gz
JRE_LINUX=jre-8u131-linux-x64.tar.gz

rm -f "${INSTALLER_COMPRESSED_FILE}"

mkdir -p "${INSTALLER_DIR}"
cp "${SCRIPTS_DIR}/${FIRST_TIME_INSTALL_SCRIPT_FILE}" "${INSTALLER_DIR}"
cp "${SCRIPTS_DIR}/${UPGRADE_SCRIPT_FILE}" "${INSTALLER_DIR}"

cp README.txt "${INSTALLER_DIR}"

echo "Manager version is ${MANAGER_VERSION}"
echo "Mongo version is ${MONGO_VERSION}"
echo "Delegate version is ${DELEGATE_VERSION}"
echo "Watcher version is ${WATCHER_VERSION}"
echo "Proxy version is ${PROXY_VERSION}"
echo "UI version is ${UI_VERSION}"
echo "Learning Engine version is ${LEARNING_ENGINE_VERSION}"

sed -i "s|MONGO_VERSION|${MONGO_VERSION}|g" "${INSTALLER_DIR}/${FIRST_TIME_INSTALL_SCRIPT_FILE}"

cp -r "../${INSTALLER_TEMPLATE_DIR}" "${INSTALLER_DIR}/"
mkdir -p "${IMAGES_DIR}"
cp "${VERSION_PROPERTIES_FILE}" "${INSTALLER_DIR}/${INSTALLER_TEMPLATE_DIR}/"

if [[ -z $1 ]]; then
   echo "No license file supplied, skipping setting the license file in the installer"
else
   echo "License file supplied, generating installer with license file $1"
   sed -i "s|harness_license|$1|g" "${CONFIG_PROPERTIES_FILE}"
fi

docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
docker pull "${MANAGER_IMAGE}"
docker pull "${LEARNING_ENGINE_IMAGE}"
docker pull "${UI_IMAGE}"
docker pull "${PROXY_IMAGE}"

docker save "${MANAGER_IMAGE}" > "${MANAGER_IMAGE_TAR}"
docker save "${LEARNING_ENGINE_IMAGE}" > "${LEARNING_ENGINE_IMAGE_TAR}"
docker save "${UI_IMAGE}" > "${UI_IMAGE_TAR}"
docker save "${PROXY_IMAGE}" > "${PROXY_IMAGE_TAR}"

curl "${JRE_SOURCE_URL}/${JRE_SOLARIS}" > "${JRE_SOLARIS}"
curl "${JRE_SOURCE_URL}/${JRE_MACOSX}" > "${JRE_MACOSX}"
curl "${JRE_SOURCE_URL}/${JRE_LINUX}" > "${JRE_LINUX}"

mv delegate.jar "${IMAGES_DIR}/"
mv watcher.jar "${IMAGES_DIR}/"
mv "${JRE_SOLARIS}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX}" "${IMAGES_DIR}/"
mv "${JRE_LINUX}" "${IMAGES_DIR}/"

tar -cvzf "${INSTALLER_COMPRESSED_FILE}" "${INSTALLER_DIR}"
rm -rf "${INSTALLER_DIR}"