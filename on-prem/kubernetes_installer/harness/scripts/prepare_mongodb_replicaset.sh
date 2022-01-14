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

yq w -i ${VALUES_DST} busyboxImage.repository "$(rv images.busybox.repository)"
yq w -i ${VALUES_DST} busyboxImage.tag "$(rv images.busybox.tag)"

yq w -i ${VALUES_DST} persistentVolume.enabled true
yq w -i ${VALUES_DST} persistentVolume.storageClass "$(rv services.mongo.storageClass)"
yq w -i ${VALUES_DST} persistentVolume.size "$(rv services.mongo.size)"

yq w -i ${VALUES_DST} resources.limits.cpu "$(rv services.mongo.resources.limits.cpu)"
yq w -i ${VALUES_DST} resources.limits.memory "$(rv services.mongo.resources.limits.memory)"
yq w -i ${VALUES_DST} resources.requests.cpu "$(rv services.mongo.resources.limits.cpu)"
yq w -i ${VALUES_DST} resources.requests.memory "$(rv services.mongo.resources.limits.memory)"

helm template -f ${INSTALLER_DIR}/values.internal.yaml . -x ${INSTALLER_DIR}/templates/harness-persistent-volume.yaml > ${INSTALLER_DIR}/output/harness-persistent-volume.yaml
helm template --name harness -f ${VALUES_DST} ${MONGODB_REPLICASET_DIR} > ${INSTALLER_DIR}/output/harness-mongodb-replicaset.yaml