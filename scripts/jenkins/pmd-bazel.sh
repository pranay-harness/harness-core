#!/usr/bin/env bash

. scripts/bazel/generate_credentials.sh

RUN_PMDS=true . scripts/bazel/bazel_script.sh
