# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e
echo "${secrets.getValue("custom-manifest-fn-test-secret")}"
echo "${serviceVariable.manifestPath}"
helm version
helm create ${serviceVariable.manifestPath}
mkdir ${serviceVariable.overrideDir}
cp ${serviceVariable.manifestPath}/values.yaml ${serviceVariable.overridesPath1}
cp ${serviceVariable.overridesPath1} ${serviceVariable.overridesPath2}
