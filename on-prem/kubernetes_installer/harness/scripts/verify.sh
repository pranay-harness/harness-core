#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
INSTALLER_DIR="$(dirname ${SCRIPT_DIR})"

if [ ! -f ${INSTALLER_DIR}/templates/harness-persistent-volume.yaml ]; then
    echo "templates/harness-persistent-volume.yaml not found.";
    echo "Please copy respective template from persistent-volume-templates directory to templates directory and rename it to harness-persistent-volume.yaml";
    exit 1;
fi
