#!/usr/bin/env bash

if [[ -z "${DEPLOY_MODE}" ]]; then
    export DEPLOY_MODE=AWS
fi

if [[ -z "${HAZELCAST_PORT}" ]]; then
    export HAZELCAST_PORT=5701
fi