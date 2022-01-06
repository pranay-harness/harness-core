#!/usr/bin/env bash
# Copyright 2019 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

BUILD=$1
ARTIFACT_FILE_NAME=watcher-capsule.jar

echo "System-Properties: version=1.0.$BUILD" >> app.mf
echo "Application-Version: version=1.0.$BUILD" >> app.mf
jar ufm ${ARTIFACT_FILE_NAME} app.mf
rm -rf app.mf
mv ${ARTIFACT_FILE_NAME} watcher.jar
