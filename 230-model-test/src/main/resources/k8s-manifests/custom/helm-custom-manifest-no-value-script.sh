# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

set -e
echo "${secrets.getValue("custom-manifest-fn-test-secret")}"
echo "${serviceVariable.manifestPath}"
helm version
helm create ${serviceVariable.manifestPath}
mkdir ${serviceVariable.overrideDir}
mv ${serviceVariable.manifestPath}/values.yaml ${serviceVariable.overridesPath1}
