#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -e

ORIGINAL_SCRIPT=`mktemp`
SCRIPT=`mktemp`
bazel run --script_path="$ORIGINAL_SCRIPT" //tools/rust/aeriform

tail -n +3 "$ORIGINAL_SCRIPT" > "$SCRIPT"
rm "$ORIGINAL_SCRIPT"

chmod 755 "$SCRIPT"
"$SCRIPT" "$@"
rm "$SCRIPT"
