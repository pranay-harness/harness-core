#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

CONFIG_FILE=/opt/harness/config.yml

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE 'logging.appenders.(type==console)'
  yq write -i $CONFIG_FILE 'logging.appenders.(type==gke-console).stackdriverLogEnabled' "true"
else
  yq delete -i $CONFIG_FILE 'logging.appenders.(type==gke-console)'
fi

# Remove the TLS connector (as ingress terminates TLS)
yq delete -i $CONFIG_FILE connectors[0]


if [[ "" != "$SERVER_PORT" ]]; then
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "$SERVER_PORT"
else
  yq write -i $CONFIG_FILE server.applicationConnectors[0].port "7100"
fi

# The config for communication with the other services
if [[ "" != "$CD_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE cdServiceClientConfig.baseUrl $CD_CLIENT_BASEURL
fi

if [[ "" != "$CI_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE ciServiceClientConfig.baseUrl $CI_CLIENT_BASEURL
fi

if [[ "" != "$NG_MANAGER_CLIENT_BASEURL" ]]; then
  yq write -i $CONFIG_FILE ngManagerClientConfig.baseUrl $NG_MANAGER_CLIENT_BASEURL
fi


# Secrets
if [[ "" != "$NEXT_GEN_MANAGER_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.ngManagerServiceSecret "$NEXT_GEN_MANAGER_SECRET"
fi

if [[ "" != "$PIPELINE_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.pipelineServiceSecret "$PIPELINE_SERVICE_SECRET"
fi

if [[ "" != "$JWT_AUTH_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.jwtAuthSecret "$JWT_AUTH_SECRET"
fi

if [[ "" != "$JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq write -i $CONFIG_FILE secrets.jwtIdentityServiceSecret "$JWT_IDENTITY_SERVICE_SECRET"
fi

if [[ "" != "$ALLOWED_ORIGINS" ]]; then
  yq delete -i $CONFIG_FILE allowedOrigins
  yq write -i $CONFIG_FILE allowedOrigins "$ALLOWED_ORIGINS"
fi
