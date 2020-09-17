#!/usr/bin/env bash

if [ ! -f cereal-killer.jar ]; then
  cd tools
  mvn clean install -pl cereal-killer
  mv cereal-killer/target/cereal-killer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../cereal-killer.jar
  mvn clean
  cd ..
fi
