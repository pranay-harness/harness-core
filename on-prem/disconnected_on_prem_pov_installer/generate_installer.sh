#!/usr/bin/env bash
set -e

HELPER_IMAGE=harness/onprem-install-builder:helper
INSTALLER_DIR=/var/harness/installer
INSTALLER_TEMPLATE_DIR_NAME=harness_disconnected_on_prem_pov_final
INSTALLER_TEMPLATE_DIR="/var/harness/${INSTALLER_TEMPLATE_DIR_NAME}"
INSTALLER_WORKING_DIR="${INSTALLER_DIR}"
GENERATE_SCRIPT_FILE=generate.sh
VERSION_PROPERTIES_FILE=version.properties

docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
docker pull "${HELPER_IMAGE}"
docker run --rm \
           -v "${PWD}":"${INSTALLER_WORKING_DIR}" \
           -v "${PWD}/../${INSTALLER_TEMPLATE_DIR_NAME}":"${INSTALLER_TEMPLATE_DIR}" \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -w "${INSTALLER_WORKING_DIR}" \
           --env-file "./${VERSION_PROPERTIES_FILE}" \
           -e DOCKERHUB_USERNAME \
           -e DOCKERHUB_PASSWORD \
           "${HELPER_IMAGE}" ./${GENERATE_SCRIPT_FILE} $1