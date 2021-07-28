#!/usr/bin/env bash

set -e

scripts/bazel/aeriform.sh prepare bazel-rule
bazel build `bazel query 'attr(tags, "aeriform", //...:*)'`