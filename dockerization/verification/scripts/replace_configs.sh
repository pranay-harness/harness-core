#!/usr/bin/env bash
if [[ -v "LOGGING_LEVEL" ]]; then
    sed -i "s|level: INFO|level: ${LOGGING_LEVEL}|" /opt/harness/verification-config.yml
fi
sed -i "s|type: h2|type: http|" /opt/harness/verification-config.yml
if [[ -v "SERVER_PORT" ]]; then
    sed -i "s|port: 7070|port: ${SERVER_PORT}|" /opt/harness/verification-config.yml
fi
sed -i 's|keyStorePath: keystore.jks||' /opt/harness/verification-config.yml
sed -i 's|keyStorePassword: password||' /opt/harness/verification-config.yml
sed -i "s|trustStorePath: \${JAVA_HOME}/jre/lib/security/cacerts||" /opt/harness/verification-config.yml
sed -i 's|certAlias: localhost||' /opt/harness/verification-config.yml
sed -i 's|validateCerts: false||' /opt/harness/verification-config.yml

#if [[ -v "UI_SERVER_URL" ]]; then
#    sed -i "s|url: https://localhost:8000|url: ${UI_SERVER_URL}|" /opt/harness/verification-config.yml
#fi

#if [[ -v "ALLOWED_ORIGINS" ]]; then
#    sed -i "s|allowedOrigins: http://localhost:8000|allowedOrigins: ${ALLOWED_ORIGINS}|" /opt/harness/verification-config.yml
#fi

#if [[ -v "ALLOWED_DOMAINS" ]]; then
#    sed -i "s|allowedDomains: harness.io, wings.software, localhost,|allowedDomains: harness.io, wings.software, ${ALLOWED_DOMAINS},|" /opt/harness/verification-config.yml
#fi

if [[ -v "MONGO_URI" ]]; then
    sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" /opt/harness/verification-config.yml
fi

if [[ -v "MANAGER_URL" ]]; then
    sed -i "s|managerUrl: https://localhost:9090/api/|managerUrl: ${MANAGER_URL}|" /opt/harness/verification-config.yml
fi

if [[ -v "ENV" ]]; then
    sed -i "s|programName: verification-saas-accesslogs|programName: verification-saas-accesslogs-${ENV}|" /opt/harness/verification-config.yml
    sed -i "s|programName: verification-service|programName: verification-service-${ENV}|" /opt/harness/verification-config.yml
fi

if [[ "${SKIP_LOGS}" == "true" ]]; then
    sed -i "s|9a3e6eac4dcdbdc41a93ca99100537df||" /opt/harness/verification-config.yml
elif [[ -v "LOGDNA_KEY" ]]; then
    sed -i "s|9a3e6eac4dcdbdc41a93ca99100537df|${LOGDNA_KEY}|" /opt/harness/verification-config.yml
fi

#if [[ -v "COMPANYNAME" ]]; then
#   sed -i "s|manager-saas|manager-${COMPANYNAME}-${DEPLOY_MODE}|" /opt/harness/verification-config.yml
#fi
#
#if [[ -v "GLOBAL_WHITELIST" ]]; then
#    sed -i "s|filters: 127.0.0.1/8|filters: ${GLOBAL_WHITELIST}|" /opt/harness/verification-config.yml
#fi
#
#if [[ -v "SMTP_HOST" ]]; then
#    sed -i "s|host_placeholder|${SMTP_HOST}|" /opt/harness/verification-config.yml
#fi
#
#if [[ -v "SMTP_USERNAME" ]]; then
#    sed -i "s|smtp_username_placeholder|${SMTP_USERNAME}|" /opt/harness/verification-config.yml
#fi
#
#if [[ -v "SMTP_PASSWORD" ]]; then
#    sed -i "s|smtp_password_placeholder|${SMTP_PASSWORD}|" /opt/harness/verification-config.yml
#fi
