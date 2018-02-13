#!/usr/bin/env bash
if [[ -v "serviceVariable.LOGGING_LEVEL" ]]; then
    sed -i "s|level: INFO|level: ${serviceVariable.LOGGING_LEVEL}|" /opt/harness/config.yml
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
sed -i 's|allowedDomains: wings.software||' /opt/harness/config.yml

if [[ -v "UI_SERVER_URL" ]]; then
    sed -i "s|url: https://localhost:8000|url: ${UI_SERVER_URL}|" /opt/harness/config.yml
fi

if [[ -v "ALLOWED_ORIGINS" ]]; then
    sed -i "s|allowedOrigins: https://localhost:8000|allowedOrigins: ${ALLOWED_ORIGINS}|" /opt/harness/config.yml
fi

if [[ -v "MONGO_URI" ]]; then
    sed -i "s|uri: mongodb://localhost:27017/harness|uri: ${MONGO_URI}|" /opt/harness/config.yml
fi

sed -i 's|9a3e6eac4dcdbdc41a93ca99100537df|4ac03b05674fc5c488e3b9b235078d5d|' /opt/harness/config.yml

if [[ -v "GRAPHITE_SERVER" ]]; then
    sed -i "s|carbon.hostedgraphite.com|${GRAPHITE_SERVER}|" /opt/harness/config.yml
fi

if [[ -v "GRAPHITE_PREFIX" ]]; then
    sed -i "s|prefix: server|prefix: ${GRAPHITE_PREFIX}|" /opt/harness/config.yml
fi

if [[ -v "WATCHER_METADATA_URL" ]]; then
    sed -i "s|watcherMetadataUrl: http://wingswatchers.s3-website-us-east-1.amazonaws.com/watcherci.txt|watcherMetadataUrl: ${WATCHER_METADATA_URL}|" /opt/harness/config.yml
fi

if [[ -v "DELEGATE_METADATA_URL" ]]; then
    sed -i "s|delegateMetadataUrl: http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt|delegateMetadataUrl: ${DELEGATE_METADATA_URL}|" /opt/harness/config.yml
fi
sed -i 's|91b01067de772de3a12d99bddeab84d82a9f05c8|${NEWRELIC_LICENSE_KEY}|' /opt/harness/newrelic.yml
