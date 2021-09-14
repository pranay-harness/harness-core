#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
INSTALLER_DIR="$(dirname ${SCRIPT_DIR})"

if [ ! -f ${INSTALLER_DIR}/templates/harness-persistent-volume.yaml ]; then
    echo "templates/harness-persistent-volume.yaml not found.";
    echo "Please copy respective template from persistent-volume-templates directory to templates directory and rename it to harness-persistent-volume.yaml";
    exit 1;
fi
