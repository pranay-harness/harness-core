#!/usr/bin/env bash

BUILD=$1
ARTIFACT_FILE_NAME=delegate-capsule.jar

echo "System-Properties: version=1.0.$BUILD" >> app.mf
echo "Application-Version: version=1.0.$BUILD" >> app.mf
jar ufm ${ARTIFACT_FILE_NAME} app.mf
rm -rf app.mf
mv ${ARTIFACT_FILE_NAME} delegate.jar