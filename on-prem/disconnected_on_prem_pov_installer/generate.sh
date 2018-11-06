#!/usr/bin/env bash
set -e

INSTALLER_DIR=harness_installer
INSTALLER_COMPRESSED_FILE=harness_installer.tar.gz
INSTALLER_TEMPLATE_DIR=harness_disconnected_on_prem_pov_final
SCRIPTS_DIR=scripts
CONFIG_PROPERTIES_FILE="${INSTALLER_DIR}/config.properties"
FIRST_TIME_INSTALL_SCRIPT_FILE=first_time_only_install_harness.sh
UPGRADE_SCRIPT_FILE=upgrade_harness.sh
VERSION_PROPERTIES_FILE=version.properties

IMAGES_DIR="${INSTALLER_DIR}/images"
MANAGER_IMAGE="harness/manager:${MANAGER_VERSION}"
VERIFICATION_SERVICE_IMAGE="harness/verification-service:${VERIFICATION_SERVICE_VERSION}"
LEARNING_ENGINE_IMAGE="harness/learning-engine-onprem:${LEARNING_ENGINE_VERSION}"
UI_IMAGE="harness/ui:${UI_VERSION}"
PROXY_IMAGE="harness/proxy:${PROXY_VERSION}"
MONGO_IMAGE="mongo:${MONGO_VERSION}"

MANAGER_IMAGE_TAR="${IMAGES_DIR}/manager.tar"
VERIFICATION_SERVICE_IMAGE_TAR="${IMAGES_DIR}/verification_service.tar"
LEARNING_ENGINE_IMAGE_TAR="${IMAGES_DIR}/learning_engine.tar"
UI_IMAGE_TAR="${IMAGES_DIR}/ui.tar"
PROXY_IMAGE_TAR="${IMAGES_DIR}/proxy.tar"
MONGO_IMAGE_TAR="${IMAGES_DIR}/mongo.tar"

JRE_SOURCE_URL=https://app.harness.io/storage/wingsdelegates/jre/8u131
JRE_SOLARIS=jre-8u131-solaris-x64.tar.gz
JRE_MACOSX=jre-8u131-macosx-x64.tar.gz
JRE_LINUX=jre-8u131-linux-x64.tar.gz

rm -f "${INSTALLER_COMPRESSED_FILE}"

rm -rf "${INSTALLER_DIR}"
mkdir -p "${INSTALLER_DIR}"
mkdir -p "${IMAGES_DIR}"
cp README.txt "${INSTALLER_DIR}"

echo "Manager version is ${MANAGER_VERSION}"
echo "Mongo version is ${MONGO_VERSION}"
echo "Verification Service version is ${VERIFICATION_SERVICE_VERSION}"
echo "Delegate version is ${DELEGATE_VERSION}"
echo "Watcher version is ${WATCHER_VERSION}"
echo "Proxy version is ${PROXY_VERSION}"
echo "UI version is ${UI_VERSION}"
echo "Learning Engine version is ${LEARNING_ENGINE_VERSION}"

cp -r ../${INSTALLER_TEMPLATE_DIR}/* ${INSTALLER_DIR}/
cp "${VERSION_PROPERTIES_FILE}" "${INSTALLER_DIR}/"

if [[ -z $1 ]]; then
   echo "No license file supplied, skipping setting the license file in the installer"
else
   echo "License file supplied, generating installer with license file $1"
   sed -i "s|harness_license|$1|g" "${CONFIG_PROPERTIES_FILE}"
fi

docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
docker pull "${MANAGER_IMAGE}"
docker pull "${VERIFICATION_SERVICE_IMAGE}"
docker pull "${LEARNING_ENGINE_IMAGE}"
docker pull "${UI_IMAGE}"
docker pull "${PROXY_IMAGE}"
docker pull "${MONGO_IMAGE}"

docker save "${MANAGER_IMAGE}" > "${MANAGER_IMAGE_TAR}"
docker save "${VERIFICATION_SERVICE_IMAGE}" > "${VERIFICATION_SERVICE_IMAGE_TAR}"
docker save "${LEARNING_ENGINE_IMAGE}" > "${LEARNING_ENGINE_IMAGE_TAR}"
docker save "${UI_IMAGE}" > "${UI_IMAGE_TAR}"
docker save "${PROXY_IMAGE}" > "${PROXY_IMAGE_TAR}"
docker save "${MONGO_IMAGE}" > "${MONGO_IMAGE_TAR}"

curl "${JRE_SOURCE_URL}/${JRE_SOLARIS}" > "${JRE_SOLARIS}"
curl "${JRE_SOURCE_URL}/${JRE_MACOSX}" > "${JRE_MACOSX}"
curl "${JRE_SOURCE_URL}/${JRE_LINUX}" > "${JRE_LINUX}"

cp delegate.jar "${IMAGES_DIR}/"
cp watcher.jar "${IMAGES_DIR}/"
mv "${JRE_SOLARIS}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX}" "${IMAGES_DIR}/"
mv "${JRE_LINUX}" "${IMAGES_DIR}/"

tar -cvzf "${INSTALLER_COMPRESSED_FILE}" "${INSTALLER_DIR}"
#rm -rf "${INSTALLER_DIR}"