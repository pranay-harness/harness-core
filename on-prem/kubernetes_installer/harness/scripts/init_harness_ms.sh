#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

echo "######################### Harness Microservices Start ##############################"

kubectl apply -f output/harness-manager.yaml
kubectl apply -f output/harness-le.yaml
kubectl apply -f output/harness-ui.yaml
kubectl apply -f output/harness-verificationservice.yaml

echo "######################### Harness Microservices End ##############################"
