#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/batch-processing-config.yml
if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE mongo.uri "$MONGO_URI"
fi
