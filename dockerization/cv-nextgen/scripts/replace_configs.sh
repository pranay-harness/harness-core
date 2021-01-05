#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/cv-nextgen-config.yml

yq delete -i /opt/harness/cv-nextgen-config.yml server.adminConnectors
yq delete -i /opt/harness/cv-nextgen-config.yml server.applicationConnectors[0]

if [[ "" != "$LOGGING_LEVEL" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml logging.level "$LOGGING_LEVEL"
fi

if [[ "" != "$VERIFICATION_PORT" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml server.applicationConnectors[0].port "$VERIFICATION_PORT"
else
  yq write -i /opt/harness/cv-nextgen-config.yml server.applicationConnectors[0].port "6060"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml mongo.uri "${MONGO_URI//\\&/&}"
fi

if [[ "" != "$MANAGER_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE managerClientConfig.baseUrl "$MANAGER_CLIENT_BASEURL"
fi

if [[ "" != "$NG_MANAGER_URL" ]]; then
  yq write -i $CONFIG_FILE nextGen.ngManagerUrl "$NG_MANAGER_URL"
fi

  yq write -i /opt/harness/cv-nextgen-config.yml server.requestLog.appenders[0].type "console"
  yq write -i /opt/harness/cv-nextgen-config.yml server.requestLog.appenders[0].threshold "TRACE"
  yq write -i /opt/harness/cv-nextgen-config.yml server.requestLog.appenders[0].target "STDOUT"

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i /opt/harness/cv-nextgen-config.yml logging.appenders[0]
  yq write -i /opt/harness/cv-nextgen-config.yml logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i /opt/harness/cv-nextgen-config.yml logging.appenders[1]
fi

if [[ "" != "$DATA_STORE" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml dataStorageMode "$DATA_STORE"
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml nextGen.managerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi

if [[ "" != "$MANAGER_JWT_AUTH_SECRET" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml managerAuthConfig.jwtAuthSecret "$MANAGER_JWT_AUTH_SECRET"
fi

if [[ "" != "$MANAGER_JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq write -i /opt/harness/cv-nextgen-config.yml managerAuthConfig.jwtIdentityServiceSecret "$MANAGER_JWT_IDENTITY_SERVICE_SECRET"
fi

if [[ "" != "$NG_MANAGER_URL" ]]; then
  yq write -i $CONFIG_FILE nextGen.ngManagerUrl "$NG_MANAGER_URL"
fi

if [[ "" != "$NOTIFICATION_BASE_URL" ]]; then
  yq write -i $CONFIG_FILE notificationClient.httpClient.baseUrl "$NOTIFICATION_BASE_URL"
fi

if [[ "" != "$NOTIFICATION_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE notificationClient.messageBroker.uri "$NOTIFICATION_MONGO_URI"
fi

if [[ "" != "$PORTAL_URL" ]]; then
  yq write -i $CONFIG_FILE portalUrl "$PORTAL_URL"
fi


if [[ "" != "$EVENTS_FRAMEWORK_REDIS_URL" ]]; then
  yq write -i $CONFIG_FILE eventsFramework.redis.redisUrl "$EVENTS_FRAMEWORK_REDIS_URL"
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