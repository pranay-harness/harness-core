#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml

#yq delete -i $CONFIG_FILE server.applicationConnectors[0]

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
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "9005"
fi


if [[ "" != "$SERVER_MAX_THREADS" ]]; then
  yq write -i $CONFIG_FILE server.maxThreads "$SERVER_MAX_THREADS"
fi

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  yq delete -i $CONFIG_FILE allowedOrigins
  yq write -i $CONFIG_FILE allowedOrigins "$ALLOWED_ORIGINS"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.uri "${MONGO_URI//\\&/&}"
fi

if [[ "" != "$MONGO_CONNECT_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.connectTimeout $MONGO_CONNECT_TIMEOUT
fi

if [[ "" != "$MONGO_SERVER_SELECTION_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.serverSelectionTimeout $MONGO_SERVER_SELECTION_TIMEOUT
fi

if [[ "" != "$MAX_CONNECTION_IDLE_TIME" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.maxConnectionIdleTime $MAX_CONNECTION_IDLE_TIME
fi

if [[ "" != "$MONGO_CONNECTIONS_PER_HOST" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.connectionsPerHost $MONGO_CONNECTIONS_PER_HOST
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.mongo.indexManagerMode $MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$MANAGER_CLIENT_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.managerServiceSecret "$MANAGER_CLIENT_SECRET"
fi

if [[ "" != "$AUTH_ENABLED" ]]; then
  yq write -i $CONFIG_FILE enableAuth "$AUTH_ENABLED"
fi

if [[ "" != "$MANAGER_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE managerClientConfig.baseUrl "$MANAGER_CLIENT_BASEURL"
fi

if [[ "" != "$SMTP_HOST" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.smtp.host "$SMTP_HOST"
fi

if [[ "" != "$SMTP_USERNAME" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.smtp.username "$SMTP_USERNAME"
fi

if [[ "" != "$SMTP_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.smtp.password "$SMTP_PASSWORD"
fi

if [[ "" != "$OVERRIDE_PREDEFINED_TEMPLATES" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.seedDataConfiguration.shouldOverrideAllPredefinedTemplates "$OVERRIDE_PREDEFINED_TEMPLATES"
fi

if [[ "" != "$MONGO_MESSAGE_BROKER_URI" ]]; then
  yq write -i $CONFIG_FILE notificationClient.messageBroker.uri "$MONGO_MESSAGE_BROKER_URI"
fi

if [[ "" != "$RBAC_URL" ]]; then
  yq write -i $CONFIG_FILE rbacServiceConfig.baseUrl "$RBAC_URL"
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.ngManagerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi

if [[ "" != "$JWT_AUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.jwtAuthSecret "$JWT_AUTH_SECRET"
fi

if [[ "" != "$JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.jwtIdentityServiceSecret "$JWT_IDENTITY_SERVICE_SECRET"
fi

if [[ "" != "$GRPC_MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.grpcClient.target $GRPC_MANAGER_TARGET
fi

if [[ "" != "$GRPC_MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE notificationServiceConfig.grpcClient.authority $GRPC_MANAGER_AUTHORITY
fi

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

if [[ "" != "$AUDIT_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.uri "${AUDIT_MONGO_URI//\\&/&}"
fi

if [[ "" != "$AUDIT_MONGO_CONNECT_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.connectTimeout $AUDIT_MONGO_CONNECT_TIMEOUT
fi

if [[ "" != "$AUDIT_MONGO_SERVER_SELECTION_TIMEOUT" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.serverSelectionTimeout $AUDIT_MONGO_SERVER_SELECTION_TIMEOUT
fi

if [[ "" != "$AUDIT_MAX_CONNECTION_IDLE_TIME" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.maxConnectionIdleTime $AUDIT_MAX_CONNECTION_IDLE_TIME
fi

if [[ "" != "$AUDIT_MONGO_CONNECTIONS_PER_HOST" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.connectionsPerHost $AUDIT_MONGO_CONNECTIONS_PER_HOST
fi

if [[ "" != "$AUDIT_MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.mongo.indexManagerMode $AUDIT_MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$ENABLE_AUDIT_SERVICE" ]]; then
  yq write -i $CONFIG_FILE auditServiceConfig.enableAuditService $ENABLE_AUDIT_SERVICE
fi
