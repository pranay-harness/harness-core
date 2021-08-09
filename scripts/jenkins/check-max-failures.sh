#!/usr/bin/env bash

. scripts/jenkins/build-cereal-killer.sh

java -jar cereal-killer.jar check $(pwd) ${1:-5}
