#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -e

bazel build `bazel query 'attr(tags, "oss", //...:*)'`

bazel run //360-cg-manager/container:manager

bazel run //120-ng-manager/container:ng_manager

bazel run //800-pipeline-service/container:pipeline_service

mkdir -p destination/dist/delegate/
mkdir -p destination/dist/watcher/
cp -f ${HOME}/.bazel-dirs/bin/260-delegate/module_deploy.jar destination/dist/delegate/delegate-capsule.jar
cp -f ${HOME}/.bazel-dirs/bin/960-watcher/module_deploy.jar destination/dist/watcher/watcher-capsule.jar
chmod 744 dockerization/delegate-proxy/setup.sh
./dockerization/delegate-proxy/setup.sh
bazel run //dockerization/delegate-proxy:delegate_proxy
