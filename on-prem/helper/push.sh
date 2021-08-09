#!/usr/bin/env bash
set -e

IMAGE_NAME=harness/onprem-install-builder
IMAGE_TAG=helper

docker push "$IMAGE_NAME:$IMAGE_TAG"