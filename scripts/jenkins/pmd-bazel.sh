#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


. scripts/bazel/generate_credentials.sh

RUN_PMDS=true . scripts/bazel/bazel_script.sh
