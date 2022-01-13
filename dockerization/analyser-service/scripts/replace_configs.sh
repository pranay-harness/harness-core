#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.


CONFIG_FILE=/opt/harness/config.yml

replace_key_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq write -i "$CONFIG_FILE" "$CONFIG_KEY" "$CONFIG_VALUE"
  fi
}

yq delete -i $CONFIG_FILE 'server.applicationConnectors.(type==https)'
yq write -i $CONFIG_FILE server.adminConnectors "[]"

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE 'logging.appenders.(type==console)'
  yq write -i $CONFIG_FILE 'logging.appenders.(type==gke-console).stackdriverLogEnabled' "true"
else
  yq delete -i $CONFIG_FILE 'logging.appenders.(type==gke-console)'
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE mongo.uri "${MONGO_URI//\\&/&}"
fi

if [[ "" != "$MONGO_CONNECT_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE mongo.connectTimeout $MONGO_CONNECT_TIMEOUT
fi

if [[ "" != "$MONGO_SERVER_SELECTION_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE mongo.serverSelectionTimeout $MONGO_SERVER_SELECTION_TIMEOUT
fi

if [[ "" != "$MAX_CONNECTION_IDLE_TIME" ]]; then
  yq write -i $CONFIG_FILE mongo.maxConnectionIdleTime $MAX_CONNECTION_IDLE_TIME
fi

if [[ "" != "$MONGO_CONNECTIONS_PER_HOST" ]]; then
  yq write -i $CONFIG_FILE mongo.connectionsPerHost $MONGO_CONNECTIONS_PER_HOST
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE mongo.indexManagerMode $MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$MONGO_TRANSACTIONS_ALLOWED" ]]; then
  yq write -i $CONFIG_FILE mongo.transactionsEnabled $MONGO_TRANSACTIONS_ALLOWED
fi


if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsFramework.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

replace_key_value eventsFramework.redis.sentinel $EVENTS_FRAMEWORK_USE_SENTINEL
replace_key_value eventsFramework.redis.envNamespace $EVENTS_FRAMEWORK_ENV_NAMESPACE
replace_key_value eventsFramework.redis.redisUrl $EVENTS_FRAMEWORK_REDIS_URL
replace_key_value eventsFramework.redis.masterName $EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME
replace_key_value eventsFramework.redis.userName $EVENTS_FRAMEWORK_REDIS_USERNAME
replace_key_value eventsFramework.redis.password $EVENTS_FRAMEWORK_REDIS_PASSWORD
replace_key_value eventsFramework.redis.sslConfig.enabled $EVENTS_FRAMEWORK_REDIS_SSL_ENABLED
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePath $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePassword $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD
