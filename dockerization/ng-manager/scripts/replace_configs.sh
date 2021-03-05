#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml

replace_key_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq write -i $CONFIG_FILE $CONFIG_KEY $CONFIG_VALUE
  fi
}

yq delete -i $CONFIG_FILE server.applicationConnectors[0]
yq write -i $CONFIG_FILE server.adminConnectors "[]"

yq delete -i $CONFIG_FILE grpcServer.connectors[0]
yq delete -i $CONFIG_FILE pmsSdkGrpcServerConfig.connectors[0]

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
else
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "7090"
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

if [[ "" != "$PMS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE pmsMongo.uri "${PMS_MONGO_URI//\\&/&}"
fi

if [[ "" != "$MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE grpcClient.target $MANAGER_TARGET
fi

if [[ "" != "$MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE grpcClient.authority $MANAGER_AUTHORITY
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE grpcServer.connectors[0].port "$GRPC_SERVER_PORT"
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i $CONFIG_FILE nextGen.managerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i $CONFIG_FILE nextGen.ngManagerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi

if [[ "" != "$USER_VERIFICATION_SECRET" ]]; then
  yq write -i $CONFIG_FILE nextGen.userVerificationSecret "$USER_VERIFICATION_SECRET"
fi

if [[ "" != "$JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE nextGen.jwtIdentityServiceSecret "$JWT_IDENTITY_SERVICE_SECRET"
fi

if [[ "" != "$AUTH_ENABLED" ]]; then
  yq write -i $CONFIG_FILE enableAuth "$AUTH_ENABLED"
fi

if [[ "" != "$MANAGER_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE managerClientConfig.baseUrl "$MANAGER_CLIENT_BASEURL"
fi

if [[ "" != "$NG_MANAGER_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE ngManagerClientConfig.baseUrl "$NG_MANAGER_CLIENT_BASEURL"
fi

if [[ "" != "$SMTP_HOST" ]]; then
  yq write -i $CONFIG_FILE smtp.host "$SMTP_HOST"
fi

if [[ "" != "$SMTP_USERNAME" ]]; then
  yq write -i $CONFIG_FILE smtp.username "$SMTP_USERNAME"
fi

if [[ "" != "$SMTP_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE smtp.password "$SMTP_PASSWORD"
fi

if [[ "" != "$JWT_AUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE nextGen.jwtAuthSecret "$JWT_AUTH_SECRET"
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_URL" ]]; then
  yq write -i $CONFIG_FILE eventsFramework.redis.redisUrl "$EVENTS_FRAMEWORK_REDIS_URL"
fi

if [[ "" != "$EVENTS_FRAMEWORK_ENV_NAMESPACE" ]]; then
  yq write -i $CONFIG_FILE eventsFramework.redis.envNamespace "$EVENTS_FRAMEWORK_ENV_NAMESPACE"
fi

if [[ "" != "$EVENTS_FRAMEWORK_USE_SENTINEL" ]]; then
  yq write -i $CONFIG_FILE eventsFramework.redis.sentinel "$EVENTS_FRAMEWORK_USE_SENTINEL"
fi

if [[ "" != "$EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME" ]]; then
  yq write -i $CONFIG_FILE eventsFramework.redis.masterName "$EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME"
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsFramework.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE pmsSdkGrpcServerConfig.connectors[0].port "$GRPC_SERVER_PORT"
fi

if [[ "" != "$SHOULD_CONFIGURE_WITH_PMS" ]]; then
  yq write -i $CONFIG_FILE shouldConfigureWithPMS $SHOULD_CONFIGURE_WITH_PMS
fi

if [[ "" != "$PMS_TARGET" ]]; then
  yq write -i $CONFIG_FILE pmsGrpcClientConfig.target $PMS_TARGET
fi

if [[ "" != "$PMS_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE pmsGrpcClientConfig.authority $PMS_AUTHORITY
fi

if [[ "" != "$HARNESS_IMAGE_USER_NAME" ]]; then
  yq write -i $CONFIG_FILE ciDefaultEntityConfiguration.harnessImageUseName $HARNESS_IMAGE_USER_NAME
fi

if [[ "" != "$HARNESS_IMAGE_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE ciDefaultEntityConfiguration.harnessImagePassword $HARNESS_IMAGE_PASSWORD
fi

if [[ "" != "$LOG_STREAMING_SERVICE_BASEURL" ]]; then
  yq write -i $CONFIG_FILE logStreamingServiceConfig.baseUrl "$LOG_STREAMING_SERVICE_BASEURL"
fi

if [[ "" != "$LOG_STREAMING_SERVICE_TOKEN" ]]; then
  yq write -i $CONFIG_FILE logStreamingServiceConfig.serviceToken "$LOG_STREAMING_SERVICE_TOKEN"
fi

replace_key_value ceAwsSetupConfig.accessKey $CE_AWS_ACCESS_KEY

replace_key_value ceAwsSetupConfig.secretKey $CE_AWS_SECRET_KEY

replace_key_value ceAwsSetupConfig.destinationBucket $CE_AWS_DESTINATION_BUCKET

replace_key_value ceAwsSetupConfig.templateURL $CE_AWS_TEMPLATE_URL

replace_key_value baseUrls.ngManager $NG_MANAGER_API_URL

replace_key_value baseUrls.ui $MANAGER_UI_URL

replace_key_value baseUrls.ngUi $NG_MANAGER_UI_URL

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi