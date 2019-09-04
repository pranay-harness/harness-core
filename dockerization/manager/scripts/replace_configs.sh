#!/usr/bin/env bash

yq delete -i /opt/harness/config.yml server.adminConnectors
yq delete -i /opt/harness/config.yml server.applicationConnectors[0]

if [[ -v "LOGGING_LEVEL" ]]; then
    yq write -i /opt/harness/config.yml logging.level $LOGGING_LEVEL
fi

if [[ -v "SERVER_PORT" ]]; then
  yq write -i /opt/harness/config.yml server.applicationConnectors[0].port $SERVER_PORT
else
  yq write -i /opt/harness/config.yml server.applicationConnectors[0].port 9090
fi

if [[ -v "UI_SERVER_URL" ]]; then
  yq write -i /opt/harness/config.yml portal.url $UI_SERVER_URL
fi

if [[ -v "ALLOWED_ORIGINS" ]]; then
  yq write -i /opt/harness/config.yml portal.allowedOrigins $ALLOWED_ORIGINS
fi

if [[ -v "MONGO_URI" ]]; then
  yq write -i /opt/harness/config.yml mongo.uri ${MONGO_URI/\\\&/\&}
fi

if [[ -v "MONGO_LOCK_URI" ]]; then
  yq write -i /opt/harness/config.yml mongo.locksUri ${MONGO_LOCK_URI/\\\&/\&}
fi

if [[ "$SKIP_LOGS" == "true" ]]; then
  yq delete -i /opt/harness/config.yml server.requestLog.appenders[0]
  yq delete -i /opt/harness/config.yml logging.appenders[1]
elif [[ -v "LOGDNA_KEY" ]]; then
  yq write -i /opt/harness/config.yml server.requestLog.appenders[0].key $LOGDNA_KEY
  yq write -i /opt/harness/config.yml logging.appenders[1].key $LOGDNA_KEY
fi

if [[ -v "WATCHER_METADATA_URL" ]]; then
  yq write -i /opt/harness/config.yml watcherMetadataUrl $WATCHER_METADATA_URL
fi

if [[ -v "DELEGATE_METADATA_URL" ]]; then
  yq write -i /opt/harness/config.yml delegateMetadataUrl $DELEGATE_METADATA_URL
fi

if [[ -v "API_URL" ]]; then
  yq write -i /opt/harness/config.yml apiUrl $API_URL
fi

if [[ -v "ENV_PATH" ]]; then
  yq write -i /opt/harness/config.yml envPath $ENV_PATH
fi

if [[ -v "DEPLOY_MODE" ]]; then
  yq write -i /opt/harness/config.yml deployMode $DEPLOY_MODE
fi

if [[ -v "KUBECTL_VERSION" ]]; then
  yq write -i /opt/harness/config.yml kubectlVersion $KUBECTL_VERSION
fi

yq write -i /opt/harness/newrelic.yml common.license_key $NEWRELIC_LICENSE_KEY

if [[ "$DISABLE_NEW_RELIC" == "true" ]]; then
  yq write -i /opt/harness/newrelic.yml common.agent_enabled false
fi

if [[ -v "jwtPasswordSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtPasswordSecret $jwtPasswordSecret
fi

if [[ -v "jwtExternalServiceSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtExternalServiceSecret $jwtExternalServiceSecret
fi

if [[ -v "jwtZendeskSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtZendeskSecret $jwtZendeskSecret
fi

if [[ -v "jwtMultiAuthSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtMultiAuthSecret $jwtMultiAuthSecret
fi

if [[ -v "jwtSsoRedirectSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtSsoRedirectSecret $jwtSsoRedirectSecret
fi

if [[ -v "jwtAuthSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtAuthSecret $jwtAuthSecret
fi

if [[ -v "jwtMarketPlaceSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtMarketPlaceSecret $jwtMarketPlaceSecret
fi

if [[ -v "jwtIdentityServiceSecret" ]]; then
  yq write -i /opt/harness/config.yml portal.jwtIdentityServiceSecret $jwtIdentityServiceSecret
fi

if [[ -v "FEATURES" ]]; then
  yq write -i /opt/harness/config.yml featuresEnabled $FEATURES
fi

if [[ -v "SAMPLE_TARGET_ENV" ]]; then
  yq write -i /opt/harness/config.yml sampleTargetEnv $SAMPLE_TARGET_ENV
fi

if [[ -v "SAMPLE_TARGET_STATUS_HOST" ]]; then
  yq write -i /opt/harness/config.yml sampleTargetStatusHost $SAMPLE_TARGET_STATUS_HOST
fi

if [[ -v "COMPANYNAME" ]]; then
  yq write -i /opt/harness/config.yml server.requestLog.appenders[0].programName manager-${COMPANYNAME}-${DEPLOY_MODE}-accesslogs
  yq write -i /opt/harness/config.yml logging.appenders[1].programName manager-${COMPANYNAME}-${DEPLOY_MODE}
fi

if [[ -v "GLOBAL_WHITELIST" ]]; then
  yq write -i /opt/harness/config.yml globalWhitelistConfig.filters $GLOBAL_WHITELIST
fi

if [[ -v "SMTP_HOST" ]]; then
  yq write -i /opt/harness/config.yml smtp.host $SMTP_HOST
fi

if [[ -v "SMTP_USERNAME" ]]; then
  yq write -i /opt/harness/config.yml smtp.username $SMTP_USERNAME
fi

if [[ -v "SMTP_PASSWORD" ]]; then
  yq write -i /opt/harness/config.yml smtp.password $SMTP_PASSWORD
fi

if [[ -v "MARKETO_ENABLED" ]]; then
  yq write -i /opt/harness/config.yml marketoConfig.enabled $MARKETO_ENABLED
fi

if [[ -v "MARKETO_URL" ]]; then
  yq write -i /opt/harness/config.yml marketoConfig.url $MARKETO_URL
fi

if [[ -v "MARKETO_CLIENT_ID" ]]; then
  yq write -i /opt/harness/config.yml marketoConfig.clientId $MARKETO_CLIENT_ID
fi

if [[ -v "MARKETO_CLIENT_SECRET" ]]; then
  yq write -i /opt/harness/config.yml marketoConfig.clientSecret $MARKETO_CLIENT_SECRET
fi

if [[ -v "SEGMENT_ENABLED" ]]; then
  yq write -i /opt/harness/config.yml segmentConfig.enabled $SEGMENT_ENABLED
fi

if [[ -v "SEGMENT_URL" ]]; then
  yq write -i /opt/harness/config.yml segmentConfig.url $SEGMENT_URL
fi

if [[ -v "SEGMENT_APIKEY" ]]; then
  yq write -i /opt/harness/config.yml segmentConfig.apiKey $SEGMENT_APIKEY
fi

if [[ -v "DELEGATE_DOCKER_IMAGE" ]]; then
  yq write -i /opt/harness/config.yml portal.delegateDockerImage $DELEGATE_DOCKER_IMAGE
fi

if [[ -v "EXECUTION_LOG_DATA_STORE" ]]; then
  yq write -i /opt/harness/config.yml executionLogStorageMode $EXECUTION_LOG_DATA_STORE
fi

if [[ -v "FILE_STORAGE" ]]; then
  yq write -i /opt/harness/config.yml fileStorageMode $FILE_STORAGE
fi

if [[ -v "CLUSTER_NAME" ]]; then
  yq write -i /opt/harness/config.yml clusterName $CLUSTER_NAME
fi

if [[ -v "BACKGROUND_SCHEDULER_CLUSTERED" ]]; then
  yq write -i /opt/harness/config.yml backgroundScheduler.clustered $BACKGROUND_SCHEDULER_CLUSTERED
fi

if [[ -v "ALLOW_TRIAL_REGISTRATION" ]]; then
  yq write -i /opt/harness/config.yml trialRegistrationAllowed $ALLOW_TRIAL_REGISTRATION
fi

if [[ -v "GITHUB_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml githubConfig.clientId $GITHUB_OAUTH_CLIENT
fi

if [[ -v "GITHUB_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml githubConfig.clientSecret $GITHUB_OAUTH_SECRET
fi

if [[ -v "GITHUB_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml githubConfig.callbackUrl $GITHUB_OAUTH_CALLBACK_URL
fi

if [[ -v "AZURE_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml azureConfig.clientId $AZURE_OAUTH_CLIENT
fi

if [[ -v "AZURE_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml azureConfig.clientSecret $AZURE_OAUTH_SECRET
fi

if [[ -v "AZURE_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml azureConfig.callbackUrl $AZURE_OAUTH_CALLBACK_URL
fi

if [[ -v "GOOGLE_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml googleConfig.clientId $GOOGLE_OAUTH_CLIENT
fi

if [[ -v "GOOGLE_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml googleConfig.clientSecret $GOOGLE_OAUTH_SECRET
fi

if [[ -v "GOOGLE_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml googleConfig.callbackUrl $GOOGLE_OAUTH_CALLBACK_URL
fi

if [[ -v "BITBUCKET_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml bitbucketConfig.clientId $BITBUCKET_OAUTH_CLIENT
fi

if [[ -v "BITBUCKET_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml bitbucketConfig.clientSecret $BITBUCKET_OAUTH_SECRET
fi

if [[ -v "BITBUCKET_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml bitbucketConfig.callbackUrl $BITBUCKET_OAUTH_CALLBACK_URL
fi

if [[ -v "GITLAB_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml gitlabConfig.clientId $GITLAB_OAUTH_CLIENT
fi

if [[ -v "GITLAB_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml gitlabConfig.clientSecret $GITLAB_OAUTH_SECRET
fi

if [[ -v "GITLAB_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml gitlabConfig.callbackUrl $GITLAB_OAUTH_CALLBACK_URL
fi

if [[ -v "LINKEDIN_OAUTH_CLIENT" ]]; then
  yq write -i /opt/harness/config.yml linkedinConfig.clientId $LINKEDIN_OAUTH_CLIENT
fi

if [[ -v "LINKEDIN_OAUTH_SECRET" ]]; then
  yq write -i /opt/harness/config.yml linkedinConfig.clientSecret $LINKEDIN_OAUTH_SECRET
fi

if [[ -v "LINKEDIN_OAUTH_CALLBACK_URL" ]]; then
  yq write -i /opt/harness/config.yml linkedinConfig.callbackUrl $LINKEDIN_OAUTH_CALLBACK_URL
fi

if [[ -v "AWS_MARKETPLACE_ACCESSKEY" ]]; then
  yq write -i /opt/harness/config.yml mktPlaceConfig.awsAccessKey $AWS_MARKETPLACE_ACCESSKEY
fi

if [[ -v "AWS_MARKETPLACE_SECRETKEY" ]]; then
  yq write -i /opt/harness/config.yml mktPlaceConfig.awsSecretKey $AWS_MARKETPLACE_SECRETKEY
fi

if [[ -v "AWS_MARKETPLACE_PRODUCTCODE" ]]; then
  yq write -i /opt/harness/config.yml mktPlaceConfig.awsMarketPlaceProductCode $AWS_MARKETPLACE_PRODUCTCODE
fi

if [[ -v "ALLOW_BLACKLISTED_EMAIL_DOMAINS" ]]; then
  yq write -i /opt/harness/config.yml blacklistedEmailDomainsAllowed $ALLOW_BLACKLISTED_EMAIL_DOMAINS
fi

if [[ -v "TIMESCALEDB_URI" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.timescaledbUrl $TIMESCALEDB_URI
fi

if [[ -v "TIMESCALEDB_USERNAME" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.timescaledbUsername $TIMESCALEDB_USERNAME
fi

if [[ -v "TIMESCALEDB_PASSWORD" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.timescaledbPassword $TIMESCALEDB_PASSWORD
fi

if [[ -v "TIMESCALEDB_CONNECT_TIMEOUT" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.connectTimeout $TIMESCALEDB_CONNECT_TIMEOUT
fi

if [[ -v "TIMESCALEDB_SOCKET_TIMEOUT" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.socketTimeout $TIMESCALEDB_SOCKET_TIMEOUT
fi

if [[ -v "TIMESCALEDB_LOGUNCLOSED" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.logUnclosedConnections $TIMESCALEDB_LOGUNCLOSED
fi

if [[ -v "TIMESCALEDB_LOGGERLEVEL" ]]; then
  yq write -i /opt/harness/config.yml timescaledb.loggerLevel $TIMESCALEDB_LOGGERLEVEL
fi

if [[ -v "STACK_DRIVER_LOGGING_ENABLED" ]]; then
  yq write -i /opt/harness/config.yml logging.appenders[2].stackdriverLogEnabled $STACK_DRIVER_LOGGING_ENABLED
fi
