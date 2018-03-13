#!/usr/bin/env bash

if [[ -z "${DEPLOY_MODE}" ]]; then
    export DEPLOY_MODE=AWS
fi