#!/usr/bin/env bash
if [[ -v "LOGGING_LEVEL" ]]; then
    sed -i "s|level: INFO|level: ${LOGGING_LEVEL}|" /opt/harness/batch-processing-config.yml
fi
if [[ -v "MONGO_URI" ]]; then
    sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" /opt/harness/batch-processing-config.yml
fi
