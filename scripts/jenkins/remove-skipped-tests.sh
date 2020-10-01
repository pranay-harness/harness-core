#!/usr/bin/env bash

. scripts/jenkins/build-cereal-killer.sh

java -jar cereal-killer.jar removeSkipped $(pwd) ${1:-0.02}
