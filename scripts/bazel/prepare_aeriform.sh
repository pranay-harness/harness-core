#!/usr/bin/env bash

set -e

scripts/bazel/aeriform.sh prepare
bazel build `bazel query 'attr(tags, "aeriform", //...:*)'`