#!/usr/bin/env bash

yq delete -i /opt/harness/verification-config.yml server.adminConnectors
yq delete -i /opt/harness/verification-config.yml server.applicationConnectors[0]

if [[ "" != "$LOGGING_LEVEL" ]]; then
  yq write -i /opt/harness/verification-config.yml logging.level "$LOGGING_LEVEL"
fi

if [[ "" != "$VERIFICATION_PORT" ]]; then
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port "$VERIFICATION_PORT"
else
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port "7070"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i /opt/harness/verification-config.yml mongo.uri "${MONGO_URI//\\&/&}"
fi


if [[ "" != "$MANAGER_URL" ]]; then
  yq write -i /opt/harness/verification-config.yml managerUrl "$MANAGER_URL"
fi

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i /opt/harness/verification-config.yml logging.appenders[0]
  yq write -i /opt/harness/verification-config.yml logging.appenders[0].stackdriverLogEnabled "true"
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[1].type "console"
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[1].threshold "TRACE"
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[1].target "STDOUT"
else
  yq delete -i /opt/harness/verification-config.yml logging.appenders[1]
fi

if [[ "" != "$ENV" ]]; then
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].programName "verification-saas-accesslogs-${ENV}"
  yq write -i /opt/harness/verification-config.yml logging.appenders[1].programName "verification-service-${ENV}"
fi

if [[ "$SKIP_LOGS" == "true" ]]; then
  yq delete -i /opt/harness/verification-config.yml server.requestLog.appenders[0]
  yq delete -i /opt/harness/verification-config.yml logging.appenders[1]
elif [[ "" != "$LOGDNA_KEY" ]]; then
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].key "$LOGDNA_KEY"
  yq write -i /opt/harness/verification-config.yml logging.appenders[1].key "$LOGDNA_KEY"
fi

if [[ "" != "$DATA_STORE" ]]; then
  yq write -i /opt/harness/verification-config.yml dataStorageMode "$DATA_STORE"
fi
