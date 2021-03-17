#!/usr/bin/env bash
#

TARGETS=`bazel query 'attr(tags, "analysis", //...:*)' 2> /dev/null`
bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} ${TARGETS} 2> /dev/null
