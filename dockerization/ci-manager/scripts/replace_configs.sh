#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/ci-manager-config.yml

yq delete -i $CONFIG_FILE server.applicationConnectors[0]
yq write -i $CONFIG_FILE server.adminConnectors "[]"

yq delete -i $CONFIG_FILE pmsSdkGrpcServerConfig.connectors[0]

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
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "7090"
fi

if [[ "" != "$MANAGER_URL" ]]; then
  yq write -i $CONFIG_FILE managerClientConfig.baseUrl "$MANAGER_URL"
fi

if [[ "" != "$NG_MANAGER_URL" ]]; then
  yq write -i $CONFIG_FILE ngManagerClientConfig.baseUrl "$NG_MANAGER_URL"
fi

if [[ "" != "$ADDON_IMAGE_TAG" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.addonImageTag "$ADDON_IMAGE_TAG"
fi
if [[ "" != "$LE_IMAGE_TAG" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.liteEngineImageTag "$LE_IMAGE_TAG"
fi
if [[ "" != "$DEFAULT_MEMORY_LIMIT" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.defaultMemoryLimit "$DEFAULT_MEMORY_LIMIT"
fi
if [[ "" != "$DEFAULT_CPU_LIMIT" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.defaultCPULimit "$DEFAULT_CPU_LIMIT"
fi
if [[ "" != "$DEFAULT_INTERNAL_IMAGE_CONNECTOR" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.defaultInternalImageConnector "$DEFAULT_INTERNAL_IMAGE_CONNECTOR"
fi
if [[ "" != "$PVC_DEFAULT_STORAGE_SIZE" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.pvcDefaultStorageSize "$PVC_DEFAULT_STORAGE_SIZE"
fi
if [[ "" != "$DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE" ]]; then
  yq write -i $CONFIG_FILE ciExecutionServiceConfig.delegateServiceEndpointVariableValue "$DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE"
fi

if [[ "" != "$SERVER_MAX_THREADS" ]]; then
  yq write -i $CONFIG_FILE server.maxThreads "$SERVER_MAX_THREADS"
fi

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  yq delete -i $CONFIG_FILE allowedOrigins
  yq write -i $CONFIG_FILE allowedOrigins "$ALLOWED_ORIGINS"
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.uri "${MONGO_URI//\\&/&}"
fi

if [[ "" != "$MANAGER_TARGET" ]]; then
  yq write -i $CONFIG_FILE managerTarget $MANAGER_TARGET
fi

if [[ "" != "$MANAGER_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE managerAuthority $MANAGER_AUTHORITY
fi

if [[ "" != "$CIMANAGER_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE cimanager-mongo.uri "$CIMANAGER_MONGO_URI"
fi

if [[ "" != "$SCM_SERVICE_URI" ]]; then
  yq write -i $CONFIG_FILE scmConnectionConfig.url "$SCM_SERVICE_URI"
fi

if [[ "" != "$LOG_SERVICE_ENDPOINT" ]]; then
  yq write -i $CONFIG_FILE logServiceConfig.baseUrl "$LOG_SERVICE_ENDPOINT"
fi

if [[ "" != "$LOG_SERVICE_GLOBAL_TOKEN" ]]; then
  yq write -i $CONFIG_FILE logServiceConfig.globalToken "$LOG_SERVICE_GLOBAL_TOKEN"
fi

if [[ "" != "$TI_SERVICE_ENDPOINT" ]]; then
  yq write -i $CONFIG_FILE tiServiceConfig.baseUrl "$TI_SERVICE_ENDPOINT"
fi


if [[ "" != "$PMS_TARGET" ]]; then
  yq write -i $CONFIG_FILE pmsGrpcClientConfig.target $PMS_TARGET
fi

if [[ "" != "$PMS_AUTHORITY" ]]; then
  yq write -i $CONFIG_FILE pmsGrpcClientConfig.authority $PMS_AUTHORITY
fi

if [[ "" != "$SHOULD_CONFIGURE_WITH_PMS" ]]; then
  yq write -i $CONFIG_FILE shouldConfigureWithPMS $SHOULD_CONFIGURE_WITH_PMS
fi

if [[ "" != "$PMS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE pmsMongo.uri "${PMS_MONGO_URI//\\&/&}"
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE pmsSdkGrpcServerConfig.connectors[0].port "$GRPC_SERVER_PORT"
fi

if [[ "" != "$TI_SERVICE_GLOBAL_TOKEN" ]]; then
  yq write -i $CONFIG_FILE tiServiceConfig.globalToken "$TI_SERVICE_GLOBAL_TOKEN"
fi

if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i $CONFIG_FILE ngManagerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi
