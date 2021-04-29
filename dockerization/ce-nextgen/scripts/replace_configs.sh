#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/config.yml

replace_key_with_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq write -i $CONFIG_FILE $CONFIG_KEY $CONFIG_VALUE
  fi
}

#
yq delete -i $CONFIG_FILE server.adminConnectors
yq delete -i $CONFIG_FILE server.applicationConnectors[0]

replace_key_with_value logging.level $LOGGING_LEVEL

replace_key_with_value server.applicationConnectors[0].port $CE_NEXTGEN_PORT

replace_key_with_value events-mongo.uri "${EVENTS_MONGO_DB_URL//\\&/&}"

replace_key_with_value ngManagerClientConfig.baseUrl $NG_MANAGER_CLIENT_BASEURL
replace_key_with_value managerClientConfig.baseUrl $MANAGER_CLIENT_BASEURL

replace_key_with_value ngManagerServiceSecret $NEXT_GEN_MANAGER_SECRET
replace_key_with_value jwtAuthSecret $JWT_AUTH_SECRET
replace_key_with_value jwtIdentityServiceSecret $JWT_IDENTITY_SERVICE_SECRET

replace_key_with_value eventsFramework.redis.sentinel $EVENTS_FRAMEWORK_USE_SENTINEL
replace_key_with_value eventsFramework.redis.envNamespace $EVENTS_FRAMEWORK_ENV_NAMESPACE
replace_key_with_value eventsFramework.redis.redisUrl $EVENTS_FRAMEWORK_REDIS_URL
replace_key_with_value eventsFramework.redis.masterName $EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME
replace_key_with_value eventsFramework.redis.userName $EVENTS_FRAMEWORK_REDIS_USERNAME
replace_key_with_value eventsFramework.redis.password $EVENTS_FRAMEWORK_REDIS_PASSWORD
replace_key_with_value eventsFramework.redis.sslConfig.enabled $EVENTS_FRAMEWORK_REDIS_SSL_ENABLED
replace_key_with_value eventsFramework.redis.sslConfig.CATrustStorePath $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH
replace_key_with_value eventsFramework.redis.sslConfig.CATrustStorePassword $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsFramework.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi