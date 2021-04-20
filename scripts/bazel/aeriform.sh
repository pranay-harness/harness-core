#!/usr/bin/env bash

ORIGINAL_SCRIPT=`mktemp`
SCRIPT=`mktemp`
bazel run --script_path="$ORIGINAL_SCRIPT" //tools/rust/aeriform

tail -n +3 "$ORIGINAL_SCRIPT" > "$SCRIPT"
rm "$ORIGINAL_SCRIPT"

chmod 755 "$SCRIPT"
"$SCRIPT" "$@"
rm "$SCRIPT"