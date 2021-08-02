#!/usr/bin/env bash

echo "REPORT_UNUSED = False" > project/flags/report_unused.bzl

bazel build \
  --strict_java_deps=error \
  --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  $(bazel query 'attr(tags, "harness", //...:*)')

if [ $? -ne 0 ]; then
  exit 1
fi

echo "REPORT_UNUSED = True" > project/flags/report_unused.bzl

echo "# start analyzing ..."
TARGETS=$(bazel query 'attr(tags, "unused_dependency", //...:*)')

bazel build -k \
  --strict_java_deps=error \
  --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  ${TARGETS} \
  2>&1 | grep "^buildozer 'remove deps\|^# "

echo "REPORT_UNUSED = False" > project/flags/report_unused.bzl