#! /bin/bash

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
