#!/usr/bin/env bash
if [[ -v "LOGGING_LEVEL" ]]; then
    sed -i "s|level: INFO|level: ${LOGGING_LEVEL}|" /opt/harness/config.yml
fi
sed -i "s|type: h2|type: http|" /opt/harness/config.yml
if [[ -v "SERVER_PORT" ]]; then
    sed -i "s|port: 9090|port: ${SERVER_PORT}|" /opt/harness/config.yml
fi
sed -i 's|keyStorePath: keystore.jks||' /opt/harness/config.yml
sed -i 's|keyStorePassword: password||' /opt/harness/config.yml
sed -i "s|trustStorePath: \${JAVA_HOME}/jre/lib/security/cacerts||" /opt/harness/config.yml
sed -i 's|certAlias: localhost||' /opt/harness/config.yml
sed -i 's|validateCerts: false||' /opt/harness/config.yml

if [[ -v "UI_SERVER_URL" ]]; then
    sed -i "s|url: https://localhost:8000|url: ${UI_SERVER_URL}|" /opt/harness/config.yml
fi

if [[ -v "ALLOWED_ORIGINS" ]]; then
    sed -i "s|allowedOrigins: http://localhost:8000|allowedOrigins: ${ALLOWED_ORIGINS}|" /opt/harness/config.yml
fi

if [[ -v "ALLOWED_DOMAINS" ]]; then
    sed -i "s|allowedDomains: harness.io, wings.software, localhost,|allowedDomains: harness.io, wings.software, ${ALLOWED_DOMAINS},|" /opt/harness/config.yml
fi

if [[ -v "MONGO_URI" ]]; then
    sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" /opt/harness/config.yml
fi

if [[ -v "MONGO_LOCK_URI" ]]; then
    sed -i "s|#locksUri: mongodb://localhost:27017/impermanent|locksUri: ${MONGO_LOCK_URI}|" /opt/harness/config.yml
fi

if [[ "${SKIP_LOGS}" == "true" ]]; then
    sed -i "s|9a3e6eac4dcdbdc41a93ca99100537df||" /opt/harness/config.yml
elif [[ -v "LOGDNA_KEY" ]]; then
    sed -i "s|9a3e6eac4dcdbdc41a93ca99100537df|${LOGDNA_KEY}|" /opt/harness/config.yml
fi

if [[ -v "WATCHER_METADATA_URL" ]]; then
    sed -i "s|watcherMetadataUrl:.*|watcherMetadataUrl: ${WATCHER_METADATA_URL}|" /opt/harness/config.yml
fi

if [[ -v "DELEGATE_METADATA_URL" ]]; then
    sed -i "s|delegateMetadataUrl:.*|delegateMetadataUrl: ${DELEGATE_METADATA_URL}|" /opt/harness/config.yml
fi

if [[ -v "API_URL" ]]; then
    sed -i "s|apiUrl:|apiUrl: ${API_URL}|" /opt/harness/config.yml
fi

if [[ -v "ENV_PATH" ]]; then
    sed -i "s|envPath:|envPath: ${ENV_PATH}|" /opt/harness/config.yml
fi

if [[ -v "DEPLOY_MODE" ]]; then
    sed -i "s|deployMode: AWS|deployMode: ${DEPLOY_MODE}|" /opt/harness/config.yml
fi

if [[ -v "KUBECTL_VERSION" ]]; then
    sed -i "s|kubectlVersion:.*|kubectlVersion: ${KUBECTL_VERSION}|" /opt/harness/config.yml
fi

sed -i "s|91b01067de772de3a12d99bddeab84d82a9f05c8|${NEWRELIC_LICENSE_KEY}|" /opt/harness/newrelic.yml

if [[ "${DISABLE_NEW_RELIC}" == "true" ]]; then
    sed -i "s|agent_enabled: true|agent_enabled: false|" /opt/harness/newrelic.yml
fi

if [[ -v "jwtPasswordSecret" ]]; then
    sed -i "s|a8SGF1CQMHN6pnCJgz32kLn1tebrXnw6MtWto8xI|${jwtPasswordSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtExternalServiceSecret" ]]; then
    sed -i "s|nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ|${jwtExternalServiceSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtZendeskSecret" ]]; then
    sed -i "s|RdL7j9ZdCz6TVSHO7obJRS6ywYLJjH8tdfPP39i4MbevKjVo|${jwtZendeskSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtMultiAuthSecret" ]]; then
    sed -i "s|5E1YekVGldTSS5Kt0GHlyWrJ6fJHmee9nXSBssefAWSOgdMwAvvbvJalnYENZ0H0EealN0CxHh34gUCN|${jwtMultiAuthSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtSsoRedirectSecret" ]]; then
    sed -i "s|qY4GXZAlPJQPEV8JCPTNhgmDmnHZSAgorzGxvOY03Xptr8N9xDfAYbwGohr2pCRLfFG69vBQaNpeTjcV|${jwtSsoRedirectSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtAuthSecret" ]]; then
    sed -i "s|dOkdsVqdRPPRJG31XU0qY4MPqmBBMk0PTAGIKM6O7TGqhjyxScIdJe80mwh5Yb5zF3KxYBHw6B3Lfzlq|${jwtAuthSecret}|" /opt/harness/config.yml
fi

if [[ -v "jwtIdentityServiceSecret" ]]; then
    sed -i "s|HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU=|${jwtIdentityServiceSecret}|" /opt/harness/config.yml
fi

if [[ -v "FEATURES" ]]; then
    sed -i "s|featuresEnabled:|featuresEnabled: ${FEATURES}|" /opt/harness/config.yml
fi

if [[ -v "SAMPLE_TARGET_ENV" ]]; then
    sed -i "s|sampleTargetEnv:|sampleTargetEnv: ${SAMPLE_TARGET_ENV}|" /opt/harness/config.yml
fi

if [[ -v "COMPANYNAME" ]]; then
   sed -i "s|manager-saas|manager-${COMPANYNAME}-${DEPLOY_MODE}|" /opt/harness/config.yml
fi

if [[ -v "GLOBAL_WHITELIST" ]]; then
    sed -i "s|filters: 127.0.0.1/8|filters: ${GLOBAL_WHITELIST}|" /opt/harness/config.yml
fi

if [[ -v "SMTP_HOST" ]]; then
    sed -i "s|host_placeholder|${SMTP_HOST}|" /opt/harness/config.yml
fi

if [[ -v "SMTP_USERNAME" ]]; then
    sed -i "s|smtp_username_placeholder|${SMTP_USERNAME}|" /opt/harness/config.yml
fi

if [[ -v "SMTP_PASSWORD" ]]; then
    sed -i "s|smtp_password_placeholder|${SMTP_PASSWORD}|" /opt/harness/config.yml
fi

if [[ -v "MARKETO_ENABLED" ]]; then
    sed -i "s|enabled: false #marketoConfigEnable|enabled: ${MARKETO_ENABLED}|" /opt/harness/config.yml
fi

if [[ -v "MARKETO_URL" ]]; then
    sed -i "s|marketo_url_place_holder|${MARKETO_URL}|" /opt/harness/config.yml
fi

if [[ -v "MARKETO_CLIENT_ID" ]]; then
    sed -i "s|marketo_client_id|${MARKETO_CLIENT_ID}|" /opt/harness/config.yml
fi

if [[ -v "MARKETO_CLIENT_SECRET" ]]; then
    sed -i "s|marketo_client_secret|${MARKETO_CLIENT_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "DELEGATE_DOCKER_IMAGE" ]]; then
    sed -i "s|delegateDockerImage:.*|delegateDockerImage: ${DELEGATE_DOCKER_IMAGE}|" /opt/harness/config.yml
fi

if [[ -v "EXECUTION_LOG_DATA_STORE" ]]; then
    sed -i "s|executionLogStorageMode: MONGO|executionLogStorageMode: ${EXECUTION_LOG_DATA_STORE}|" /opt/harness/config.yml
fi

if [[ -v "FILE_STORAGE" ]]; then
    sed -i "s|fileStorageMode: MONGO|fileStorageMode: ${FILE_STORAGE}|" /opt/harness/config.yml
fi

if [[ -v "CLUSTER_NAME" ]]; then
    sed -i "s|clusterName:|clusterName: ${CLUSTER_NAME}|" /opt/harness/config.yml
fi

if [[ -v "BACKGROUND_SCHEDULER_CLUSTERED" ]]; then
    sed -i "s|clustered: true #backgroundScheduler|clustered: ${BACKGROUND_SCHEDULER_CLUSTERED}|" /opt/harness/config.yml
fi

if [[ -v "ALLOW_TRIAL_REGISTRATION" ]]; then
    sed -i "s|trialRegistrationAllowed: true|trialRegistrationAllowed: ${ALLOW_TRIAL_REGISTRATION}|" /opt/harness/config.yml
fi

if [[ -v "GITHUB_OAUTH_CLIENT" ]]; then
    sed -i "s|githubClientId|${GITHUB_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "GITHUB_OAUTH_SECRET" ]]; then
    sed -i "s|githubClientSecret|${GITHUB_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "GITHUB_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|githubCallbackUrl|${GITHUB_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "AZURE_OAUTH_CLIENT" ]]; then
    sed -i "s|azureClientId|${AZURE_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "AZURE_OAUTH_SECRET" ]]; then
    sed -i "s|azureClientSecret|${AZURE_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "AZURE_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|azureCallbackUrl|${AZURE_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "GOOGLE_OAUTH_CLIENT" ]]; then
    sed -i "s|googleClientId|${GOOGLE_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "GOOGLE_OAUTH_SECRET" ]]; then
    sed -i "s|googleClientSecret|${GOOGLE_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "GOOGLE_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|googleCallbackUrl|${GOOGLE_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "BITBUCKET_OAUTH_CLIENT" ]]; then
    sed -i "s|bitbucketClientId|${BITBUCKET_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "BITBUCKET_OAUTH_SECRET" ]]; then
    sed -i "s|bitbucketClientSecret|${BITBUCKET_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "BITBUCKET_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|bitbucketCallbackUrl|${BITBUCKET_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "GITLAB_OAUTH_CLIENT" ]]; then
    sed -i "s|gitlabClientId|${GITLAB_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "GITLAB_OAUTH_SECRET" ]]; then
    sed -i "s|gitlabClientSecret|${GITLAB_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "GITLAB_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|gitlabCallbackUrl|${GITLAB_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "LINKEDIN_OAUTH_CLIENT" ]]; then
    sed -i "s|linkedinClientId|${LINKEDIN_OAUTH_CLIENT}|" /opt/harness/config.yml
fi

if [[ -v "LINKEDIN_OAUTH_SECRET" ]]; then
    sed -i "s|linkedinClientSecret|${LINKEDIN_OAUTH_SECRET}|" /opt/harness/config.yml
fi

if [[ -v "LINKEDIN_OAUTH_CALLBACK_URL" ]]; then
    sed -i "s|linkedinCallbackUrl|${LINKEDIN_OAUTH_CALLBACK_URL}|" /opt/harness/config.yml
fi

if [[ -v "AWS_MARKETPLACE_ACCESSKEY" ]]; then
    sed -i "s|awsMktPlaceAccessKeyPlaceHolder|${AWS_MARKETPLACE_ACCESSKEY}|" /opt/harness/config.yml
fi

if [[ -v "AWS_MARKETPLACE_SECRETKEY" ]]; then
    sed -i "s|awsMktPlaceSecretKeyPlaceHolder|${AWS_MARKETPLACE_SECRETKEY}|" /opt/harness/config.yml
fi

if [[ -v "AWS_MARKETPLACE_PRODUCTCODE" ]]; then
    sed -i "s|awsMktPlaceProductCodePlaceHolder|${AWS_MARKETPLACE_PRODUCTCODE}|" /opt/harness/config.yml
fi

if [[ -v "ALLOW_BLACKLISTED_EMAIL_DOMAINS" ]]; then
    sed -i "s|blacklistedEmailDomainsAllowed: true|blacklistedEmailDomainsAllowed: ${ALLOW_BLACKLISTED_EMAIL_DOMAINS}|" /opt/harness/config.yml
fi
