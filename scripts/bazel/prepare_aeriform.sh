#!/usr/bin/env bash

scripts/bazel/aeriform.sh prepare
bazel build `bazel query 'attr(tags, "aeriform", //...:*)'`