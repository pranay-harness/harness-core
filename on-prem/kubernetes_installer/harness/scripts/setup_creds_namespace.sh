#!/usr/bin/env bash

set -e

kubectl apply -f output/harness-namespace.yaml

kubectl apply -f output/harness-regcred.yaml
