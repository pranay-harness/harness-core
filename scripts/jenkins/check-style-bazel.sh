#!/usr/bin/env bash

. scripts/bazel/generate_credentials.sh

RUN_CHECKS=true . scripts/bazel/bazel_script.sh
