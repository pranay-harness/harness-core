#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -e

echo "######################### Harness Microservices Start ##############################"

kubectl apply -f output/harness-manager.yaml
kubectl apply -f output/harness-le.yaml
kubectl apply -f output/harness-ui.yaml
kubectl apply -f output/harness-verificationservice.yaml

echo "######################### Harness Microservices End ##############################"
