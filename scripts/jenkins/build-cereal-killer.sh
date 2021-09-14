#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


if [ ! -f cereal-killer.jar ]; then
  cd tools
  mvn clean install -pl cereal-killer
  mv cereal-killer/target/cereal-killer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../cereal-killer.jar
  mvn clean
  cd ..
fi
