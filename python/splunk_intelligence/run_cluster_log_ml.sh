#! /bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


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
