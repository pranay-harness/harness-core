#! /bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


BASEDIR=$(dirname "$0")
cd $BASEDIR

echo $@
#Running locally
if [ -d "dist" ]; then
    source .pyenv/bin/activate; cd dist/splunk_pyml; python TimeSeriesML.pyc $@
else
    cd $SPLUNKML_ROOT
    source .pyenv/bin/activate; python TimeSeriesML.pyc $@
fi
