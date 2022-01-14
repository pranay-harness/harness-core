# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

mkdir -p ${serviceVariable.overridesPath}
cd ${serviceVariable.overridesPath}

cat << OVERRIDE3 > params3
PARAM3: values3-override
PARAM4: values4-override
OVERRIDE3

cat << OVERRIDE4 > params4
PARAM4: values4-override
OVERRIDE4

ABSOLUTE_PATH_OVERRIDES_PATH="${serviceVariable.absolutePath}/${serviceVariable.overridesPath}"

if [ -f  "$ABSOLUTE_PATH_OVERRIDES_PATH" ]; then
  rm -rf "$ABSOLUTE_PATH_OVERRIDES_PATH"
fi

mkdir -p "$ABSOLUTE_PATH_OVERRIDES_PATH"

cat << OVERRIDE3 > "$ABSOLUTE_PATH_OVERRIDES_PATH/params3"
PARAM3: values3-override
PARAM4: values4-override
OVERRIDE3

cat << OVERRIDE4 > "$ABSOLUTE_PATH_OVERRIDES_PATH/params4"
PARAM4: values4-override
OVERRIDE4
