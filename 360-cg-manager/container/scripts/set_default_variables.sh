#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


if [[ -z "${DEPLOY_MODE}" ]]; then
    export DEPLOY_MODE=AWS
fi

if [[ -z "${HAZELCAST_PORT}" ]]; then
    export HAZELCAST_PORT=5701
fi
