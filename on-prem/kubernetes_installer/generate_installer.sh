#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

set -e

HELPER_IMAGE=harness/onprem-install-builder:helper
INSTALLER_DIR=/opt/harness/installer
INSTALLER_WORKING_DIR="${INSTALLER_DIR}"
GENERATE_SCRIPT_FILE=generate.sh

docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
docker pull "${HELPER_IMAGE}"
docker run --rm -it\
           -v "${PWD}":"${INSTALLER_WORKING_DIR}" \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -w "${INSTALLER_WORKING_DIR}" \
           -e DOCKERHUB_USERNAME \
           -e DOCKERHUB_PASSWORD \
           "${HELPER_IMAGE}" ./${GENERATE_SCRIPT_FILE}

echo ""
echo "Installer generated."
