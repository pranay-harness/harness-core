#!/usr/bin/env bash

yq delete -i /opt/harness/verification-config.yml server.adminConnectors
yq delete -i /opt/harness/verification-config.yml server.applicationConnectors[0]

if [[ -v "LOGGING_LEVEL" ]]; then
  yq write -i /opt/harness/verification-config.yml logging.level $LOGGING_LEVEL
fi

if [[ -v "VERIFICATION_PORT" ]]; then
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port $VERIFICATION_PORT
else
  yq write -i /opt/harness/verification-config.yml server.applicationConnectors[0].port 7070
fi

if [[ -v "MONGO_URI" ]]; then
  yq write -i /opt/harness/verification-config.yml mongo.uri ${MONGO_URI/\\\&/\&}
fi


if [[ -v "MANAGER_URL" ]]; then
  yq write -i /opt/harness/verification-config.yml managerUrl $MANAGER_URL
fi

if [[ -v "ENV" ]]; then
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].programName verification-saas-accesslogs-${ENV}
  yq write -i /opt/harness/verification-config.yml logging.appenders[1].programName verification-service-${ENV}
fi

if [[ "$SKIP_LOGS" == "true" ]]; then
  yq delete -i /opt/harness/verification-config.yml server.requestLog.appenders[0]
  yq delete -i /opt/harness/verification-config.yml logging.appenders[1]
elif [[ -v "LOGDNA_KEY" ]]; then
  yq write -i /opt/harness/verification-config.yml server.requestLog.appenders[0].key $LOGDNA_KEY
  yq write -i /opt/harness/verification-config.yml logging.appenders[1].key $LOGDNA_KEY
fi

if [[ -v "DATA_STORE" ]]; then
  yq write -i /opt/harness/verification-config.yml dataStorageMode $DATA_STORE
fi

if [[ -v "STACK_DRIVER_LOGGING_ENABLED" ]]; then
  yq write -i /opt/harness/verification-config.yml logging.appenders[2].stackdriverLogEnabled $STACK_DRIVER_LOGGING_ENABLED
fi
