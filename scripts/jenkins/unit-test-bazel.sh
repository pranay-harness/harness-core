#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


. scripts/bazel/generate_credentials.sh \
&& RUN_BAZEL_TESTS=true . scripts/bazel/bazel_script.sh
exit $?
