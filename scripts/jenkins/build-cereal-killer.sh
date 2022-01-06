#!/usr/bin/env bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [ ! -f cereal-killer.jar ]; then
  cd tools
  mvn clean install -pl cereal-killer
  mv cereal-killer/target/cereal-killer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../cereal-killer.jar
  mvn clean
  cd ..
fi
