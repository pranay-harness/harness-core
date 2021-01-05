#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/config.yml

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

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE mongo.indexManagerMode $MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE grpcServerConfig.connectors[0].port "$GRPC_SERVER_PORT"
fi

if [[ "" != "$MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE managerTarget $MANAGER_TARGET
fi

if [[ "" != "$MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE managerAuthority $MANAGER_AUTHORITY
fi

if [[ "" != "$NG_MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE grpcClientConfigs.cd.target $NG_MANAGER_TARGET
fi

if [[ "" != "$NG_MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE grpcClientConfigs.cd.authority $NG_MANAGER_AUTHORITY
fi

if [[ "" != "$CV_MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE grpcClientConfigs.cv.target $CV_MANAGER_TARGET
fi

if [[ "" != "$CV_MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE grpcClientConfigs.cv.authority $CV_MANAGER_AUTHORITY
fi

if [[ "" != "$SCM_SERVICE_URI" ]]; then
  yq write -i $CONFIG_FILE scmConnectionConfig.url "$SCM_SERVICE_URI"
fi
