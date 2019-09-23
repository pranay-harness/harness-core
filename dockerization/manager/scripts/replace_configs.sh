#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml
NEWRELIC_FILE=/opt/harness/newrelic.yml

yq delete -i $CONFIG_FILE server.adminConnectors
yq delete -i $CONFIG_FILE server.applicationConnectors[0]

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

if [[ "" != "$UI_SERVER_URL" ]]; then
  yq write -i $CONFIG_FILE portal.url "$UI_SERVER_URL"
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

if [[ "" != "$ELASTICSEARCH_URI" ]]; then
  yq write -i $CONFIG_FILE elasticsearch.uri "$ELASTICSEARCH_URI"
fi

if [[ "" != "$MONGO_LOCK_URI" ]]; then
  yq write -i $CONFIG_FILE mongo.locksUri "${MONGO_LOCK_URI//\\&/&}"
fi

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
  yq write -i $CONFIG_FILE server.requestLog.appenders[1].type "console"
  yq write -i $CONFIG_FILE server.requestLog.appenders[1].threshold "TRACE"
  yq write -i $CONFIG_FILE server.requestLog.appenders[1].target "STDOUT"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

if [[ "" != "$COMPANYNAME" ]]; then
  yq write -i $CONFIG_FILE server.requestLog.appenders[0].programName "manager-${COMPANYNAME}-${DEPLOY_MODE}-accesslogs"
  yq write -i $CONFIG_FILE logging.appenders[1].programName "manager-${COMPANYNAME}-${DEPLOY_MODE}"
fi

if [[ "$SKIP_LOGS" == "true" ]]; then
  yq delete -i $CONFIG_FILE server.requestLog.appenders[0]
  yq delete -i $CONFIG_FILE logging.appenders[1]
elif [[ "" != "$LOGDNA_KEY" ]]; then
  yq write -i $CONFIG_FILE server.requestLog.appenders[0].key "$LOGDNA_KEY"
  yq write -i $CONFIG_FILE logging.appenders[1].key "$LOGDNA_KEY"
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
