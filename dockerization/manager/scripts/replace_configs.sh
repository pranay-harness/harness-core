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

if [[ -v "DEPLOY_MODE" ]]; then
    sed -i "s|deployMode: AWS|deployMode: ${DEPLOY_MODE}|" /opt/harness/config.yml
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

if [[ -v "FEATURES" ]]; then
    sed -i "s|featuresEnabled:|featuresEnabled: ${FEATURES}|" /opt/harness/config.yml
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

if [[ -v "DELEGATE_DOCKER_IMAGE" ]]; then
    sed -i "s|delegateDockerImage:.*|delegateDockerImage: ${DELEGATE_DOCKER_IMAGE}|" /opt/harness/config.yml
fi
