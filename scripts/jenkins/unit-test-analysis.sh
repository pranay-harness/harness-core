#!/usr/bin/env bash

. scripts/bazel/generate_credentials.sh \
&& RUN_BAZEL_ANALYSIS=true . scripts/bazel/bazel_script.sh
