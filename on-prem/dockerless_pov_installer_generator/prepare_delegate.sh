#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


BUILD=$1
ARTIFACT_FILE_NAME=delegate-capsule.jar

echo "System-Properties: version=1.0.$BUILD" >> app.mf
echo "Application-Version: version=1.0.$BUILD" >> app.mf
jar ufm ${ARTIFACT_FILE_NAME} app.mf
rm -rf app.mf
mv ${ARTIFACT_FILE_NAME} delegate.jar
