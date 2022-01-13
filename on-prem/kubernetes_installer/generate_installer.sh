#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
