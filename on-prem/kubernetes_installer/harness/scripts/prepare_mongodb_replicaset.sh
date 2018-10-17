#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
INSTALLER_DIR="$(dirname ${SCRIPT_DIR})"

source ${SCRIPT_DIR}/utils.sh

MONGODB_REPLICASET_DIR="$INSTALLER_DIR/components/mongodb-replicaset"
VALUES_DST="$MONGODB_REPLICASET_DIR/values.yaml"

yq w -i ${VALUES_DST} auth.enabled true
yq w -i ${VALUES_DST} auth.adminUser "$(rv services.mongo.adminUser)"
yq w -i ${VALUES_DST} auth.adminPassword "$(rv services.mongo.adminPassword)"
yq w -i ${VALUES_DST} auth.key "$(rv services.mongo.key)"

yq w -i ${VALUES_DST} installImage.repository "$(rv images.mongoInstall.repository)"
yq w -i ${VALUES_DST} installImage.tag "$(rv images.mongoInstall.tag)"

yq w -i ${VALUES_DST} image.repository "$(rv images.mongo.repository)"
yq w -i ${VALUES_DST} image.tag "$(rv images.mongo.tag)"

yq w -i ${VALUES_DST} persistentVolume.enabled false

helm template --name harness -f ${VALUES_DST} ${MONGODB_REPLICASET_DIR} > ${INSTALLER_DIR}/output/harness-mongodb-replicaset.yaml
