#!/usr/bin/env bash

. scripts/jenkins/build-cereal-killer.sh

java -jar cereal-killer.jar suppressFlakes $(pwd) ${1:-0.02}
