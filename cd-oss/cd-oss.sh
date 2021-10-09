#!/usr/bin/env bash

set -e

cd ..
BAZEL_BIN=`bazel info bazel-bin`

bazel build `bazel query 'attr(tags, "oss", //...:*)'`

bazel run //360-cg-manager/container:manager

bazel run //120-ng-manager/container:ng_manager

bazel run //800-pipeline-service/container:pipeline_service

bazel run //820-platform-service/container:platform_service

export VERSION_FILE=build.properties
export VERSION=`cat ${VERSION_FILE} | grep 'build.number=' | sed -e 's: *build.number=::g'`
echo $VERSION

mkdir -p destination/dist/delegate/
mkdir -p destination/dist/watcher/
cp -f ${BAZEL_BIN}/260-delegate/module_deploy.jar destination/dist/delegate/delegate-capsule.jar
cp -f ${BAZEL_BIN}/960-watcher/module_deploy.jar destination/dist/watcher/watcher-capsule.jar
chmod 744 dockerization/delegate-proxy/setup.sh
./dockerization/delegate-proxy/setup.sh
bazel run //dockerization/delegate-proxy:delegate_proxy