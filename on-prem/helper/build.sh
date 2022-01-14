#!/usr/bin/env bash
set -e

IMAGE_NAME=harness/onprem-install-builder
IMAGE_TAG=helper
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
docker build -t "$IMAGE_NAME:$IMAGE_TAG" "$SCRIPT_DIR"