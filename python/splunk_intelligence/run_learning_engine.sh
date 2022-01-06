#! /bin/bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

BASEDIR=$(dirname "$0")
cd $BASEDIR

echo $@
#Running locally
if [ -d "dist" ]; then
    source .pyenv/bin/activate; cd dist/splunk_pyml; python LearningEngine.pyc $@
else
    cd $SPLUNKML_ROOT
    if [ ${EXPERIMENTAL} == "true" ]; then
        python LearningEngineExperimental.pyc $@
     else
        python LearningEngine.pyc $@
     fi
fi
