#!/usr/bin/env bash

set -e

bazel build `bazel query 'attr(tags, "oss", //...:*)'`

bazel run //360-cg-manager/container:manager

bazel run //120-ng-manager/container:ng_manager

bazel run //800-pipeline-service/container:pipeline_service