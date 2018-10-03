#!/usr/bin/env bash

set -e

echo "######################### Harness Microservices Start ##############################"

kubectl apply -f output/harness-manager.yaml
kubectl apply -f output/harness-le.yaml
kubectl apply -f output/harness-ui.yaml

echo "######################### Harness Microservices End ##############################"
