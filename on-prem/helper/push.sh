#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

set -e

IMAGE_NAME=harness/onprem-install-builder
IMAGE_TAG=helper

docker push "$IMAGE_NAME:$IMAGE_TAG"
