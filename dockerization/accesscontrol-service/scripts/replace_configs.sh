#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml

yq delete -i $CONFIG_FILE server.applicationConnectors[0]

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

if [[ "" != "$LOGGING_LEVEL" ]]; then
    yq write -i $CONFIG_FILE logging.level "$LOGGING_LEVEL"
fi

if [[ "" != "$LOGGERS" ]]; then
  IFS=',' read -ra LOGGER_ITEMS <<< "$LOGGERS"
  for ITEM in "${LOGGER_ITEMS[@]}"; do
    LOGGER=`echo $ITEM | awk -F= '{print $1}'`
    LOGGER_LEVEL=`echo $ITEM | awk -F= '{print $2}'`
    yq write -i $CONFIG_FILE logging.loggers.[$LOGGER] "${LOGGER_LEVEL}"
  done
fi

if [[ "" != "$SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "$SERVER_PORT"
fi

if [[ "" != "$SERVER_MAX_THREADS" ]]; then
  yq write -i $CONFIG_FILE server.maxThreads "$SERVER_MAX_THREADS"
fi

if [[ "" != "$EVENTS_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsConfig.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$LOCK_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$LOCK_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE redisLockConfig.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  IFS=',' read -ra ALLOWED_ORIGINS <<< "$ALLOWED_ORIGINS"
  INDEX=0
  for ALLOWED_URL in "${ALLOWED_ORIGINS[@]}"; do
    yq write -i $CONFIG_FILE allowedOrigins.[$INDEX] "${ALLOWED_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi