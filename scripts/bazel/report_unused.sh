#!/usr/bin/env bash

TARGETS=""
TARGETS="$TARGETS //250-watcher/..."
TARGETS="$TARGETS //925-watcher-beans/..."
TARGETS="$TARGETS //970-grpc/..."
TARGETS="$TARGETS //980-commons/..."
TARGETS="$TARGETS //990-commons-test/..."

echo "REPORT_UNUSED = False" > project/flags/report_unused.bzl

bazel build \
  --strict_java_deps=error \
  --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  $TARGETS

if [ $? -ne 0 ]; then
  exit 1
fi

echo "REPORT_UNUSED = True" > project/flags/report_unused.bzl

echo "start analyzing ..."
bazel build \
  --strict_java_deps=error \
  --host_java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  --java_toolchain=@bazel_tools//tools/jdk:toolchain_hostjdk8 \
  $TARGETS -k 2>&1 | grep "^buildozer 'remove deps\|^# "

echo "REPORT_UNUSED = False" > project/flags/report_unused.bzl