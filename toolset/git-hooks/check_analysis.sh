#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

#

TARGETS=`bazel query 'attr(tags, "analysis", //...:*)' 2> /dev/null`
bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} ${TARGETS} 2> /dev/null
