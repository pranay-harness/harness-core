#!/usr/bin/env bash

. scripts/bazel/generate_credentials.sh \
&& RUN_BAZEL_TESTS_NEW=true . scripts/bazel/bazel_script.sh
exit $?
