#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/config.yml

write_mongo_hosts_and_ports() {
  IFS=',' read -ra HOST_AND_PORT <<< "$2"
  for INDEX in "${!HOST_AND_PORT[@]}"; do
    HOST=$(cut -d: -f 1 <<< "${HOST_AND_PORT[$INDEX]}")
    PORT=$(cut -s -d: -f 2 <<< "${HOST_AND_PORT[$INDEX]}")
    yq write -i $CONFIG_FILE $1.hosts[$INDEX].host "$HOST"
    if [ -n "$PORT" ]; then
      yq write -i $CONFIG_FILE $1.hosts[$INDEX].port "$PORT"
    fi
  done
}

write_mongo_params() {
  IFS=',' read -ra PARAMS <<< "$2"
  for PARAM_PAIR in "${PARAMS[@]}"; do
    NAME=$(cut -d= -f 1 <<< "$PARAM_PAIR")
    VALUE=$(cut -d= -f 2 <<< "$PARAM_PAIR")
    yq write -i $CONFIG_FILE $1.params.$NAME "$VALUE"
  done
}

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi

# Remove the TLS connector (as ingress terminates TLS)
yq delete -i $CONFIG_FILE connectors[0]

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.uri "$MONGO_URI"
fi

if [[ "" != "$MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE harness-mongo.uri
  yq write -i $CONFIG_FILE harness-mongo.username "$MONGO_USERNAME"
  yq write -i $CONFIG_FILE harness-mongo.password "$MONGO_PASSWORD"
  yq write -i $CONFIG_FILE harness-mongo.database "$MONGO_DATABASE"
  write_mongo_hosts_and_ports harness-mongo "$MONGO_HOSTS_AND_PORTS"
  write_mongo_params harness-mongo "$MONGO_PARAMS"
fi

if [[ "" != "$MONGO_TAG_NAME" ]]; then
  yq write -i $CONFIG_FILE mongotags.tagKey "$MONGO_TAG_NAME"
fi

if [[ "" != "$MONGO_TAG_VALUE" ]]; then
  yq write -i $CONFIG_FILE mongotags.tagValue "$MONGO_TAG_VALUE"
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.indexManagerMode $MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$EVEMTS_MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE events-mongo.indexManagerMode $EVEMTS_MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$EVENTS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE events-mongo.uri "$EVENTS_MONGO_URI"
fi

if [[ "" != "$EVENTS_MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE events-mongo.uri
  yq write -i $CONFIG_FILE events-mongo.username "$EVENTS_MONGO_USERNAME"
  yq write -i $CONFIG_FILE events-mongo.password "$EVENTS_MONGO_PASSWORD"
  yq write -i $CONFIG_FILE events-mongo.database "$EVENTS_MONGO_DATABASE"
  write_mongo_hosts_and_ports events-mongo "$EVENTS_MONGO_HOSTS_AND_PORTS"
  write_mongo_params events-mongo "$EVENTS_MONGO_PARAMS"
fi

if [[ "" != "$PMS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE pms-harness.uri "$PMS_MONGO_URI"
fi

if [[ "" != "$PMS_MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE pms-harness.uri
  yq write -i $CONFIG_FILE pms-harness.username "$PMS_MONGO_USERNAME"
  yq write -i $CONFIG_FILE pms-harness.password "$PMS_MONGO_PASSWORD"
  yq write -i $CONFIG_FILE pms-harness.database "$PMS_MONGO_DATABASE"
  write_mongo_hosts_and_ports pms-harness "$PMS_MONGO_HOSTS_AND_PORTS"
  write_mongo_params pms-harness "$PMS_MONGO_PARAMS"
fi

if [[ "" != "$CDC_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE cdc-mongo.uri "$CDC_MONGO_URI"
fi

if [[ "" != "$CDC_MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE cdc-mongo.uri
  yq write -i $CONFIG_FILE cdc-mongo.username "$CDC_MONGO_USERNAME"
  yq write -i $CONFIG_FILE cdc-mongo.password "$CDC_MONGO_PASSWORD"
  yq write -i $CONFIG_FILE cdc-mongo.database "$CDC_MONGO_DATABASE"
  write_mongo_hosts_and_ports cdc-mongo "$CDC_MONGO_HOSTS_AND_PORTS"
  write_mongo_params cdc-mongo "$CDC_MONGO_PARAMS"
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

if [[ "" != "$GCP_PROJECT_ID" ]]; then
  yq write -i $CONFIG_FILE gcp-project-id "$GCP_PROJECT_ID"
fi

if [[ "" != "$NG_HARNESS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE ng-harness.uri  "$NG_HARNESS_MONGO_URI"
fi

if [[ "" != "$NG_HARNESS_MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE ng-harness.uri
  yq write -i $CONFIG_FILE ng-harness.username "$NG_HARNESS_MONGO_USERNAME"
  yq write -i $CONFIG_FILE ng-harness.password "$NG_HARNESS_MONGO_PASSWORD"
  yq write -i $CONFIG_FILE ng-harness.database "$NG_HARNESS_MONGO_DATABASE"
  write_mongo_hosts_and_ports ng-harness "$NG_HARNESS_MONGO_HOSTS_AND_PORTS"
  write_mongo_params ng-harness "$NG_HARNESS_MONGO_PARAMS"
fi
