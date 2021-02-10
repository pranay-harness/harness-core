#!/usr/bin/env bash

bazel build //... `bazel query 'attr(tags, "aeriform", //...:*)'`