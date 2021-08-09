#!/usr/bin/env bash

. scripts/jenkins/build-cereal-killer.sh

java -jar cereal-killer.jar uploadMetrics $1
