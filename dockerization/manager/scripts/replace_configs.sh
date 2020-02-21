#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml
NEWRELIC_FILE=/opt/harness/newrelic.yml

yq delete -i $CONFIG_FILE server.adminConnectors
yq delete -i $CONFIG_FILE server.applicationConnectors[0]
yq delete -i $CONFIG_FILE grpcServerConfig.connectors[0]

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
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "9090"
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE grpcServerConfig.connectors[0].port "$GRPC_SERVER_PORT"
fi

if [[ "" != "$SERVER_MAX_THREADS" ]]; then
  yq write -i $CONFIG_FILE server.maxThreads "$SERVER_MAX_THREADS"
fi

if [[ "" != "$UI_SERVER_URL" ]]; then
  yq write -i $CONFIG_FILE portal.url "$UI_SERVER_URL"
fi

if [[ "" != "$AUTHTOKENEXPIRYINMILLIS" ]]; then
  yq write -i $CONFIG_FILE portal.authTokenExpiryInMillis "$AUTHTOKENEXPIRYINMILLIS"
fi

if [[ "" != "$EXTERNAL_GRAPHQL_RATE_LIMIT" ]]; then
  yq write -i $CONFIG_FILE portal.externalGraphQLRateLimitPerMinute "$EXTERNAL_GRAPHQL_RATE_LIMIT"
fi

if [[ "" != "$CUSTOM_DASH_GRAPHQL_RATE_LIMIT" ]]; then
  yq write -i $CONFIG_FILE portal.customDashGraphQLRateLimitPerMinute "$CUSTOM_DASH_GRAPHQL_RATE_LIMIT"
fi

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  yq write -i $CONFIG_FILE portal.allowedOrigins "$ALLOWED_ORIGINS"
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

if [[ "" != "$EVENTS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE events-mongo.uri "$EVENTS_MONGO_URI"
else
  yq delete -i $CONFIG_FILE events-mongo
fi

if [[ "" != "$ELASTICSEARCH_URI" ]]; then
  yq write -i $CONFIG_FILE elasticsearch.uri "$ELASTICSEARCH_URI"
fi

if [[ "" != "$ELASTICSEARCH_INDEX_SUFFIX" ]]; then
  yq write -i $CONFIG_FILE elasticsearch.indexSuffix "$ELASTICSEARCH_INDEX_SUFFIX"
fi

if [[ "" != "$ELASTICSEARCH_MONGO_TAG_NAME" ]]; then
 yq write -i $CONFIG_FILE elasticsearch.mongoTagKey "$ELASTICSEARCH_MONGO_TAG_NAME"
fi

if [[ "" != "$ELASTICSEARCH_MONGO_TAG_VALUE" ]]; then
 yq write -i $CONFIG_FILE elasticsearch.mongoTagValue "$ELASTICSEARCH_MONGO_TAG_VALUE"
fi

if [[ "" != "$MONGO_LOCK_URI" ]]; then
  yq write -i $CONFIG_FILE mongo.locksUri "${MONGO_LOCK_URI//\\&/&}"
fi

yq write -i $CONFIG_FILE server.requestLog.appenders[0].type "console"
yq write -i $CONFIG_FILE server.requestLog.appenders[0].threshold "TRACE"
yq write -i $CONFIG_FILE server.requestLog.appenders[0].target "STDOUT"

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

if [[ "" != "$WATCHER_METADATA_URL" ]]; then
  yq write -i $CONFIG_FILE watcherMetadataUrl "$WATCHER_METADATA_URL"
fi

if [[ "" != "$DELEGATE_METADATA_URL" ]]; then
  yq write -i $CONFIG_FILE delegateMetadataUrl "$DELEGATE_METADATA_URL"
fi

if [[ "" != "$API_URL" ]]; then
  yq write -i $CONFIG_FILE apiUrl "$API_URL"
fi

if [[ "" != "$ENV_PATH" ]]; then
  yq write -i $CONFIG_FILE envPath "$ENV_PATH"
fi

if [[ "" != "$DEPLOY_MODE" ]]; then
  yq write -i $CONFIG_FILE deployMode "$DEPLOY_MODE"
fi

if [[ "" != "$KUBECTL_VERSION" ]]; then
  yq write -i $CONFIG_FILE kubectlVersion "$KUBECTL_VERSION"
fi

yq write -i $NEWRELIC_FILE common.license_key "$NEWRELIC_LICENSE_KEY"

if [[ "$DISABLE_NEW_RELIC" == "true" ]]; then
  yq write -i $NEWRELIC_FILE common.agent_enabled false
fi

if [[ "" != "$jwtPasswordSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtPasswordSecret "$jwtPasswordSecret"
fi

if [[ "" != "$jwtExternalServiceSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtExternalServiceSecret "$jwtExternalServiceSecret"
fi

if [[ "" != "$jwtZendeskSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtZendeskSecret "$jwtZendeskSecret"
fi

if [[ "" != "$jwtMultiAuthSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtMultiAuthSecret "$jwtMultiAuthSecret"
fi

if [[ "" != "$jwtSsoRedirectSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtSsoRedirectSecret "$jwtSsoRedirectSecret"
fi

if [[ "" != "$jwtAuthSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtAuthSecret "$jwtAuthSecret"
fi

if [[ "" != "$jwtMarketPlaceSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtMarketPlaceSecret "$jwtMarketPlaceSecret"
fi

if [[ "" != "$jwtIdentityServiceSecret" ]]; then
  yq write -i $CONFIG_FILE portal.jwtIdentityServiceSecret "$jwtIdentityServiceSecret"
fi

if [[ "" != "$FEATURES" ]]; then
  yq write -i $CONFIG_FILE featuresEnabled "$FEATURES"
fi

if [[ "" != "$SAMPLE_TARGET_ENV" ]]; then
  yq write -i $CONFIG_FILE sampleTargetEnv "$SAMPLE_TARGET_ENV"
fi

if [[ "" != "$SAMPLE_TARGET_STATUS_HOST" ]]; then
  yq write -i $CONFIG_FILE sampleTargetStatusHost "$SAMPLE_TARGET_STATUS_HOST"
fi

if [[ "" != "$GLOBAL_WHITELIST" ]]; then
  yq write -i $CONFIG_FILE globalWhitelistConfig.filters "$GLOBAL_WHITELIST"
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

if [[ "" != "$MARKETO_ENABLED" ]]; then
  yq write -i $CONFIG_FILE marketoConfig.enabled "$MARKETO_ENABLED"
fi

if [[ "" != "$MARKETO_URL" ]]; then
  yq write -i $CONFIG_FILE marketoConfig.url "$MARKETO_URL"
fi

if [[ "" != "$MARKETO_CLIENT_ID" ]]; then
  yq write -i $CONFIG_FILE marketoConfig.clientId "$MARKETO_CLIENT_ID"
fi

if [[ "" != "$MARKETO_CLIENT_SECRET" ]]; then
  yq write -i $CONFIG_FILE marketoConfig.clientSecret "$MARKETO_CLIENT_SECRET"
fi

if [[ "" != "$SEGMENT_ENABLED" ]]; then
  yq write -i $CONFIG_FILE segmentConfig.enabled "$SEGMENT_ENABLED"
fi

if [[ "" != "$SEGMENT_URL" ]]; then
  yq write -i $CONFIG_FILE segmentConfig.url "$SEGMENT_URL"
fi

if [[ "" != "$SEGMENT_APIKEY" ]]; then
  yq write -i $CONFIG_FILE segmentConfig.apiKey "$SEGMENT_APIKEY"
fi

if [[ "" != "$SALESFORCE_USERNAME" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.userName "$SALESFORCE_USERNAME"
fi

if [[ "" != "$SALESFORCE_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.password "$SALESFORCE_PASSWORD"
fi

if [[ "" != "$SALESFORCE_CONSUMER_KEY" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.consumerKey "$SALESFORCE_CONSUMER_KEY"
fi

if [[ "" != "$SALESFORCE_CONSUMER_SECRET" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.consumerSecret "$SALESFORCE_CONSUMER_SECRET"
fi

if [[ "" != "$SALESFORCE_GRANT_TYPE" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.grantType "$SALESFORCE_GRANT_TYPE"
fi

if [[ "" != "$SALESFORCE_LOGIN_INSTANCE_DOMAIN" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.loginInstanceDomain "$SALESFORCE_LOGIN_INSTANCE_DOMAIN"
fi

if [[ "" != "$SALESFORCE_API_VERSION" ]]; then
  yq write -i $CONFIG_FILE salesforceConfig.apiVersion "$SALESFORCE_API_VERSION"
fi

if [[ "" != "$DATADOG_ENABLED" ]]; then
  yq write -i $CONFIG_FILE datadogConfig.enabled "$DATADOG_ENABLED"
fi

if [[ "" != "$DATADOG_APIKEY" ]]; then
  yq write -i $CONFIG_FILE datadogConfig.apiKey "$DATADOG_APIKEY"
fi

if [[ "" != "$DELEGATE_DOCKER_IMAGE" ]]; then
  yq write -i $CONFIG_FILE portal.delegateDockerImage "$DELEGATE_DOCKER_IMAGE"
fi

if [[ "" != "$EXECUTION_LOG_DATA_STORE" ]]; then
  yq write -i $CONFIG_FILE executionLogStorageMode "$EXECUTION_LOG_DATA_STORE"
fi

if [[ "" != "$FILE_STORAGE" ]]; then
  yq write -i $CONFIG_FILE fileStorageMode "$FILE_STORAGE"
fi

if [[ "" != "$CLUSTER_NAME" ]]; then
  yq write -i $CONFIG_FILE clusterName "$CLUSTER_NAME"
fi

if [[ "" != "$BACKGROUND_SCHEDULER_CLUSTERED" ]]; then
  yq write -i $CONFIG_FILE backgroundScheduler.clustered "$BACKGROUND_SCHEDULER_CLUSTERED"
fi

if [[ "" != "$ENABLE_CRONS" ]]; then
  yq write -i $CONFIG_FILE enableIterators "$ENABLE_CRONS"
  yq write -i $CONFIG_FILE backgroundScheduler.enabled "$ENABLE_CRONS"
  yq write -i $CONFIG_FILE serviceScheduler.enabled "$ENABLE_CRONS"
fi

if [[ "" != "$ALLOW_TRIAL_REGISTRATION" ]]; then
  yq write -i $CONFIG_FILE trialRegistrationAllowed "$ALLOW_TRIAL_REGISTRATION"
fi

if [[ "" != "$ALLOW_TRIAL_REGISTRATION_FOR_HACATHON" ]]; then
  yq write -i $CONFIG_FILE trialRegistrationAllowedForHackathon "$ALLOW_TRIAL_REGISTRATION_FOR_HACATHON"
fi

if [[ "" != "$GITHUB_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE githubConfig.clientId "$GITHUB_OAUTH_CLIENT"
fi

if [[ "" != "$GITHUB_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE githubConfig.clientSecret "$GITHUB_OAUTH_SECRET"
fi

if [[ "" != "$GITHUB_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE githubConfig.callbackUrl "$GITHUB_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$AZURE_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE azureConfig.clientId "$AZURE_OAUTH_CLIENT"
fi

if [[ "" != "$AZURE_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE azureConfig.clientSecret "$AZURE_OAUTH_SECRET"
fi

if [[ "" != "$AZURE_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE azureConfig.callbackUrl "$AZURE_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$GOOGLE_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE googleConfig.clientId "$GOOGLE_OAUTH_CLIENT"
fi

if [[ "" != "$GOOGLE_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE googleConfig.clientSecret "$GOOGLE_OAUTH_SECRET"
fi

if [[ "" != "$GOOGLE_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE googleConfig.callbackUrl "$GOOGLE_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$BITBUCKET_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE bitbucketConfig.clientId "$BITBUCKET_OAUTH_CLIENT"
fi

if [[ "" != "$BITBUCKET_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE bitbucketConfig.clientSecret "$BITBUCKET_OAUTH_SECRET"
fi

if [[ "" != "$BITBUCKET_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE bitbucketConfig.callbackUrl "$BITBUCKET_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$GITLAB_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE gitlabConfig.clientId "$GITLAB_OAUTH_CLIENT"
fi

if [[ "" != "$GITLAB_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE gitlabConfig.clientSecret "$GITLAB_OAUTH_SECRET"
fi

if [[ "" != "$GITLAB_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE gitlabConfig.callbackUrl "$GITLAB_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$LINKEDIN_OAUTH_CLIENT" ]]; then
  yq write -i $CONFIG_FILE linkedinConfig.clientId "$LINKEDIN_OAUTH_CLIENT"
fi

if [[ "" != "$LINKEDIN_OAUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE linkedinConfig.clientSecret "$LINKEDIN_OAUTH_SECRET"
fi

if [[ "" != "$LINKEDIN_OAUTH_CALLBACK_URL" ]]; then
  yq write -i $CONFIG_FILE linkedinConfig.callbackUrl "$LINKEDIN_OAUTH_CALLBACK_URL"
fi

if [[ "" != "$AWS_MARKETPLACE_ACCESSKEY" ]]; then
  yq write -i $CONFIG_FILE mktPlaceConfig.awsAccessKey "$AWS_MARKETPLACE_ACCESSKEY"
fi

if [[ "" != "$AWS_MARKETPLACE_SECRETKEY" ]]; then
  yq write -i $CONFIG_FILE mktPlaceConfig.awsSecretKey "$AWS_MARKETPLACE_SECRETKEY"
fi

if [[ "" != "$AWS_MARKETPLACE_PRODUCTCODE" ]]; then
  yq write -i $CONFIG_FILE mktPlaceConfig.awsMarketPlaceProductCode "$AWS_MARKETPLACE_PRODUCTCODE"
fi

if [[ "" != "$ALLOW_BLACKLISTED_EMAIL_DOMAINS" ]]; then
  yq write -i $CONFIG_FILE blacklistedEmailDomainsAllowed "$ALLOW_BLACKLISTED_EMAIL_DOMAINS"
fi

if [[ "" != "$ALLOW_PWNED_PASSWORDS" ]]; then
  yq write -i $CONFIG_FILE pwnedPasswordsAllowed "$ALLOW_PWNED_PASSWORDS"
fi

if [[ "" != "$TIMESCALEDB_URI" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbUrl "$TIMESCALEDB_URI"
fi

if [[ "" != "$TIMESCALEDB_USERNAME" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbUsername "$TIMESCALEDB_USERNAME"
fi

if [[ "" != "$TIMESCALEDB_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbPassword "$TIMESCALEDB_PASSWORD"
fi

if [[ "" != "$TIMESCALEDB_CONNECT_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE timescaledb.connectTimeout "$TIMESCALEDB_CONNECT_TIMEOUT"
fi

if [[ "" != "$TIMESCALEDB_SOCKET_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE timescaledb.socketTimeout "$TIMESCALEDB_SOCKET_TIMEOUT"
fi

if [[ "" != "$TIMESCALEDB_LOGUNCLOSED" ]]; then
  yq write -i $CONFIG_FILE timescaledb.logUnclosedConnections "$TIMESCALEDB_LOGUNCLOSED"
fi

if [[ "" != "$TIMESCALEDB_LOGGERLEVEL" ]]; then
  yq write -i $CONFIG_FILE timescaledb.loggerLevel "$TIMESCALEDB_LOGGERLEVEL"
fi

if [[ "$SEARCH_ENABLED" == "true" ]]; then
  yq write -i $CONFIG_FILE searchEnabled true
fi

if [[ "$MONGO_DEBUGGING_ENABLED" == "true" ]]; then
  yq write -i $CONFIG_FILE logging.loggers.[org.mongodb.morphia.query] TRACE
  yq write -i $CONFIG_FILE logging.loggers.connection TRACE
fi

if [[ "" != "$AZURE_MARKETPLACE_ACCESSKEY" ]]; then
  yq write -i $CONFIG_FILE mktPlaceConfig.azureMarketplaceAccessKey "$AZURE_MARKETPLACE_ACCESSKEY"
fi

if [[ "" != "$AZURE_MARKETPLACE_SECRETKEY" ]]; then
  yq write -i $CONFIG_FILE mktPlaceConfig.azureMarketplaceSecretKey "$AZURE_MARKETPLACE_SECRETKEY"
fi

if [[ "" != "$WORKERS" ]]; then
  IFS=',' read -ra WORKER_ITEMS <<< "$WORKERS"
  for ITEM in "${WORKER_ITEMS[@]}"; do
    WORKER=`echo $ITEM | awk -F= '{print $1}'`
    WORKER_FLAG=`echo $ITEM | awk -F= '{print $2}'`
    yq write -i $CONFIG_FILE workers.active.[$WORKER] "${WORKER_FLAG}"
  done
fi

if [[ "" != "$PUBLISHERS" ]]; then
  IFS=',' read -ra PUBLISHER_ITEMS <<< "$PUBLISHERS"
  for ITEM in "${PUBLISHER_ITEMS[@]}"; do
    PUBLISHER=`echo $ITEM | awk -F= '{print $1}'`
    PUBLISHER_FLAG=`echo $ITEM | awk -F= '{print $2}'`
    yq write -i $CONFIG_FILE publishers.active.[$PUBLISHER] "${PUBLISHER_FLAG}"
  done
fi

if [[ "" != "$DISTRIBUTED_LOCK_IMPLEMENTATION" ]]; then
  yq write -i $CONFIG_FILE distributedLockImplementation "$DISTRIBUTED_LOCK_IMPLEMENTATION"
fi

if [[ "$REDIS_SENTINEL" == "true" ]]; then
  yq write -i $CONFIG_FILE redisConfig.sentinel true
fi

if [[ "" != "$REDIS_URL" ]]; then
  yq write -i $CONFIG_FILE redisConfig.redisUrl "$REDIS_URL"
fi

if [[ "" != "$REDIS_MASTER_NAME" ]]; then
  yq write -i $CONFIG_FILE redisConfig.masterName "$REDIS_MASTER_NAME"
fi

if [[ "" != "$REDIS_SENTINELS" ]]; then
  IFS=',' read -ra REDIS_SENTINEL_URLS <<< "$REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${REDIS_SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE redisConfig.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$REDIS_ENV_NAMESPACE" ]]; then
    yq write -i $CONFIG_FILE redisConfig.envNamespace "$REDIS_ENV_NAMESPACE"
fi
