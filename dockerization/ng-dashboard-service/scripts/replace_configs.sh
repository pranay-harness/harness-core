#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/config.yml

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

# Remove the TLS connector (as ingress terminates TLS)
yq delete -i $CONFIG_FILE connectors[0]
