#!/usr/bin/env bash -x

. scripts/bazel/generate_credentials.sh

RUN_CHECKS=true . scripts/bazel/bazel_script.sh

#RUN_PMDS=true . scripts/bazel/bazel_script.sh
