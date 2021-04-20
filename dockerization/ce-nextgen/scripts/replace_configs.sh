#!/usr/bin/env bash
CONFIG_FILE=/opt/harness/config.yml

replace_key_with_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq write -i $CONFIG_FILE $CONFIG_KEY $CONFIG_VALUE
  fi
}

#
yq delete -i $CONFIG_FILE server.adminConnectors
yq delete -i $CONFIG_FILE server.applicationConnectors[0]

replace_key_with_value logging.level $LOGGING_LEVEL

replace_key_with_value server.applicationConnectors[0].port $CE_NEXTGEN_PORT

replace_key_with_value events-mongo.uri "${EVENTS_MONGO_DB_URL//\\&/&}"

replace_key_with_value ngManagerClientConfig.baseUrl $NG_MANAGER_CLIENT_BASEURL
replace_key_with_value managerClientConfig.baseUrl $MANAGER_CLIENT_BASEURL

replace_key_with_value ngManagerServiceSecret $NEXT_GEN_MANAGER_SECRET
replace_key_with_value jwtAuthSecret $JWT_AUTH_SECRET
replace_key_with_value jwtIdentityServiceSecret $JWT_IDENTITY_SERVICE_SECRET