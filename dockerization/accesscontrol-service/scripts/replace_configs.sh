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

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  yq delete -i $CONFIG_FILE allowedOrigins
  yq write -i $CONFIG_FILE allowedOrigins "$ALLOWED_ORIGINS"
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

if [[ "" != "$MONGO_TRANSACTIONS_ENABLED" ]]; then
  yq write -i $CONFIG_FILE mongo.transactionsEnabled $MONGO_TRANSACTIONS_ENABLED
fi

if [[ "" != "$MONGO_ALIAS_DB_NAME" ]]; then
  yq write -i $CONFIG_FILE mongo.aliasDBName "$MONGO_ALIAS_DB_NAME"
fi

if [[ "" != "$EVENTS_CONFIG_REDIS_URL" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.redisUrl "$EVENTS_CONFIG_REDIS_URL"
fi

if [[ "" != "$EVENTS_CONFIG_ENV_NAMESPACE" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.envNamespace "$EVENTS_CONFIG_ENV_NAMESPACE"
fi

if [[ "" != "$EVENTS_CONFIG_USE_SENTINEL" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.sentinel "$EVENTS_CONFIG_USE_SENTINEL"
fi

if [[ "" != "$EVENTS_CONFIG_SENTINEL_MASTER_NAME" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.masterName "$EVENTS_CONFIG_SENTINEL_MASTER_NAME"
fi

if [[ "" != "$EVENTS_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsConfig.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$EVENTS_CONFIG_REDIS_USERNAME" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.userName "$EVENTS_CONFIG_REDIS_USERNAME"
fi

if [[ "" != "$EVENTS_CONFIG_REDIS_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE eventsConfig.redis.password "$EVENTS_CONFIG_REDIS_PASSWORD"
fi

if [[ "" != "$RESOURCE_GROUP_ITERATOR_ENABLED" ]]; then
  yq write -i $CONFIG_FILE iteratorsConfig.resourceGroupIteratorConfig.enabled $RESOURCE_GROUP_ITERATOR_ENABLED
fi

if [[ "" != "$RESOURCE_GROUP_ITERATOR_INTERVAL" ]]; then
  yq write -i $CONFIG_FILE iteratorsConfig.resourceGroupIteratorConfig.targetIntervalInSeconds $RESOURCE_GROUP_ITERATOR_INTERVAL
fi

if [[ "" != "$AGGREGATOR_ENABLED" ]]; then
  yq write -i $CONFIG_FILE aggregatorModuleConfig.enabled $AGGREGATOR_ENABLED
fi

if [[ "" != "$ACCESS_CONTROL_CLIENT_BASE_URL" ]]; then
  yq write -i $CONFIG_FILE accessControlClient.accessControlServiceConfig.baseUrl "$ACCESS_CONTROL_CLIENT_BASE_URL"
fi

if [[ "" != "$ACCESS_CONTROL_CLIENT_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE accessControlClient.accessControlServiceSecret "$ACCESS_CONTROL_CLIENT_SERVICE_SECRET"
fi

if [[ "" != "$USER_CLIENT_BASE_URL" ]]; then
  yq write -i $CONFIG_FILE userClient.userServiceConfig.baseUrl "$USER_CLIENT_BASE_URL"
fi

if [[ "" != "$USER_CLIENT_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE userClient.userServiceSecret "$USER_CLIENT_SERVICE_SECRET"
fi

if [[ "" != "$RESOURCE_GROUP_CLIENT_BASE_URL" ]]; then
  yq write -i $CONFIG_FILE resourceGroupClient.resourceGroupServiceConfig.baseUrl "$RESOURCE_GROUP_CLIENT_BASE_URL"
fi

if [[ "" != "$RESOURCE_GROUP_CLIENT_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE resourceGroupClient.resourceGroupServiceSecret "$RESOURCE_GROUP_CLIENT_SERVICE_SECRET"
fi

if [[ "" != "$ENABLE_AUTH" ]]; then
  yq write -i $CONFIG_FILE enableAuth "$ENABLE_AUTH"
fi

if [[ "" != "$DEFAULT_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE defaultServiceSecret "$DEFAULT_SERVICE_SECRET"
fi

if [[ "" != "$JWT_AUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE jwtAuthSecret "$JWT_AUTH_SECRET"
fi

if [[ "" != "$IDENTITY_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE identityServiceSecret "$IDENTITY_SERVICE_SECRET"
fi
