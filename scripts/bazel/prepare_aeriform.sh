#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -e

scripts/bazel/aeriform.sh prepare bazel-rule
bazel build `bazel query 'attr(tags, "aeriform", //...:*)'`
