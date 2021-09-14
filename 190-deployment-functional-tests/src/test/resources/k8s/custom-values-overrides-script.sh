# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

mkdir -p ${serviceVariable.overridesPath}
cd ${serviceVariable.overridesPath}

cat << OVERRIDE3 > values3.yaml
overrides:
  value3: values3-override
  value4: values4-override
OVERRIDE3

cat << OVERRIDE4 > values4.yaml
overrides:
  value4: values4-override
OVERRIDE4

ABSOLUTE_PATH_OVERRIDES_PATH="${serviceVariable.absolutePath}/${serviceVariable.overridesPath}"

if [ -f  "$ABSOLUTE_PATH_OVERRIDES_PATH" ]; then
  rm -rf "$ABSOLUTE_PATH_OVERRIDES_PATH"
fi

mkdir -p "$ABSOLUTE_PATH_OVERRIDES_PATH"

cat << OVERRIDE3 > "$ABSOLUTE_PATH_OVERRIDES_PATH/values3.yaml"
overrides:
  value3: values3-override
  value4: values4-override
OVERRIDE3

cat << OVERRIDE4 > "$ABSOLUTE_PATH_OVERRIDES_PATH/values4.yaml"
overrides:
  value4: values4-override
OVERRIDE4
