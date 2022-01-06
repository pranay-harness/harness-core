#! /bin/bash
# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

BASEDIR=$(dirname "$0")
cd $BASEDIR

#env="x"${SPLUNKML_ENVIRONMENT}
#echo $env
#if [ $env == "x" ]; then
#    if [ ! -d ".pyenv" ]; then
#       easy_install virtualenv
#       make init
#    fi
#    make dist
#fi

echo $@
#Running locally
if [ -d "dist" ]; then
    source .pyenv/bin/activate; cd dist/splunk_pyml; python ClusterInput.pyc $@
else
    cd $SPLUNKML_ROOT
    source .pyenv/bin/activate; python ClusterInput.pyc $@
fi
