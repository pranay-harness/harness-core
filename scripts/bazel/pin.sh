#!/usr/bin/env bash

REPIN=1 bazel run @unpinned_maven//:pin
REPIN=1 bazel run @unpinned_z_harness_maven//:pin
