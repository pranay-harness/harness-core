#!/usr/bin/env bash

bazel build //tools/rust/aeriform `bazel query 'attr(tags, "aeriform", //...:*)'`