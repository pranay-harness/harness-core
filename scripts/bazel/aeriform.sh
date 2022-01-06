#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

ORIGINAL_SCRIPT=`mktemp`
SCRIPT=`mktemp`
bazel run --script_path="$ORIGINAL_SCRIPT" //tools/rust/aeriform

tail -n +3 "$ORIGINAL_SCRIPT" > "$SCRIPT"
rm "$ORIGINAL_SCRIPT"

chmod 755 "$SCRIPT"
"$SCRIPT" "$@"
rm "$SCRIPT"
