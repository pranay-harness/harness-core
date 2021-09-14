#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -e

kubectl apply -f output/harness-namespace.yaml

kubectl apply -f output/harness-regcred.yaml
