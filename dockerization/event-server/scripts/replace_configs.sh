#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/event-service-config.yml

if [[ "" != "$LOGGING_LEVEL" ]]; then
    yq write -i $CONFIG_FILE logging.level "$LOGGING_LEVEL"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE mongo.uri "$MONGO_URI"
fi
