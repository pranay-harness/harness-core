#!/usr/bin/env bash
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

CONFIG_FILE=/opt/harness/event-service-config.yml

write_mongo_hosts_and_ports() {
  IFS=',' read -ra HOST_AND_PORT <<< "$2"
  for INDEX in "${!HOST_AND_PORT[@]}"; do
    HOST=$(cut -d: -f 1 <<< "${HOST_AND_PORT[$INDEX]}")
    PORT=$(cut -d: -f 2 -s <<< "${HOST_AND_PORT[$INDEX]}")

    yq write -i $CONFIG_FILE $1.hosts[$INDEX].host "$HOST"
    if [[ "" != "$PORT" ]]; then
      yq write -i $CONFIG_FILE $1.hosts[$INDEX].port "$PORT"
    fi
  done
}

write_mongo_params() {
  IFS='&' read -ra PARAMS <<< "$2"
  for PARAM_PAIR in "${PARAMS[@]}"; do
    NAME=$(cut -d= -f 1 <<< "$PARAM_PAIR")
    VALUE=$(cut -d= -f 2 <<< "$PARAM_PAIR")
    yq write -i $CONFIG_FILE $1.params.$NAME "$VALUE"
  done
}

# Remove the TLS connector (as ingress terminates TLS)
yq delete -i $CONFIG_FILE 'connectors.(secure==true)'

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.uri "$MONGO_URI"
fi

if [[ "" != "$MONGO_HOSTS_AND_PORTS" ]]; then
  yq delete -i $CONFIG_FILE harness-mongo.uri
  yq write -i $CONFIG_FILE harness-mongo.username "$MONGO_USERNAME"
  yq write -i $CONFIG_FILE harness-mongo.password "$MONGO_PASSWORD"
  yq write -i $CONFIG_FILE harness-mongo.database "$MONGO_DATABASE"
  yq write -i $CONFIG_FILE harness-mongo.schema "$MONGO_SCHEMA"
  write_mongo_hosts_and_ports harness-mongo "$MONGO_HOSTS_AND_PORTS"
  write_mongo_params harness-mongo "$MONGO_PARAMS"
fi

if [[ "" != "$MONGO_READ_PREF_NAME" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.readPref.name "$MONGO_READ_PREF_NAME"
fi

if [[ "" != "$MONGO_READ_PREF_TAGS" ]]; then
  IFS=',' read -ra TAG_ITEMS <<< "$MONGO_READ_PREF_TAGS"
  for ITEM in "${TAG_ITEMS[@]}"; do
    TAG_NAME=$(echo $ITEM | awk -F= '{print $1}')
    TAG_VALUE=$(echo $ITEM | awk -F= '{print $2}')
    yq write -i $CONFIG_FILE "harness-mongo.readPref.tagSet.[$TAG_NAME]" "$TAG_VALUE"
  done
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
  yq write -i $CONFIG_FILE events-mongo.schema "$EVENTS_MONGO_SCHEMA"
  write_mongo_hosts_and_ports events-mongo "$EVENTS_MONGO_HOSTS_AND_PORTS"
  write_mongo_params events-mongo "$EVENTS_MONGO_PARAMS"
fi

if [[ "" != "$GCP_SECRET_MANAGER_PROJECT" ]]; then
  yq write -i $CONFIG_FILE secretsConfiguration.gcpSecretManagerProject "$GCP_SECRET_MANAGER_PROJECT"
fi

if [[ "" != "$RESOLVE_SECRETS" ]]; then
  yq write -i $CONFIG_FILE secretsConfiguration.secretResolutionEnabled "$RESOLVE_SECRETS"
fi
