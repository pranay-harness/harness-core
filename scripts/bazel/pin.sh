#!/usr/bin/env bash

REPIN=1 bazel run @unpinned_maven//:pin
REPIN=1 bazel run @unpinned_maven_test//:pin
REPIN=1 bazel run @unpinned_maven_delegate//:pin