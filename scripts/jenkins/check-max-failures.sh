#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


. scripts/jenkins/build-cereal-killer.sh

java -jar cereal-killer.jar check $(pwd) ${1:-5}
