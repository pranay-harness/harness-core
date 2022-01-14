#!/usr/bin/env bash

. scripts/bazel/generate_credentials.sh \
&& RUN_BAZEL_TESTS=true . scripts/bazel/bazel_script.sh
exit $?
