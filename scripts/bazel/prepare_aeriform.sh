#!/usr/bin/env bash

bazel build //... `bazel query 'attr(tags, "aeriform", //...:*)'`

bazel build //tools/rust/aeriform