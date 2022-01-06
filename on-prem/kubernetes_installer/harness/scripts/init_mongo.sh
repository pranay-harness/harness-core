#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
K8S_CLUSTER_NAMESPACE=$(yq r ${SCRIPT_DIR}/../values.internal.yaml kubernetesClusterNamespace)
source ${SCRIPT_DIR}/utils.sh

echo "######################### Mongo Start ##############################"
kubectl patch serviceaccount default -n ${K8S_CLUSTER_NAMESPACE} -p '{"imagePullSecrets": [{"name": "regcred"}]}'

if [[ !$(kubectl get configmaps -n ${K8S_CLUSTER_NAMESPACE} scripts-configmap) ]]; then
    echo "No configs found setting config"
    kubectl apply -f output/harness-configs.yaml
fi

kubectl apply -n ${K8S_CLUSTER_NAMESPACE} -f output/harness-persistent-volume.yaml
kubectl apply -n ${K8S_CLUSTER_NAMESPACE} -f output/harness-mongodb-replicaset.yaml

echo "Waiting for mongo replicaset to be ready..."

echo "Mongo replicaset pods status: [0/3]"
until kubectl get pods harness-mongodb-replicaset-0 -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' -n ${K8S_CLUSTER_NAMESPACE} 2>/dev/null | grep True &> /dev/null; do sleep 3; done;
echo "Mongo replicaset pods status: [1/3]"
until kubectl get pods harness-mongodb-replicaset-1 -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' -n ${K8S_CLUSTER_NAMESPACE} 2>/dev/null | grep True &> /dev/null; do sleep 3; done;
echo "Mongo replicaset pods status: [2/3]"
until kubectl get pods harness-mongodb-replicaset-2 -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' -n ${K8S_CLUSTER_NAMESPACE} 2>/dev/null | grep True &> /dev/null; do sleep 3; done;
echo "Mongo replicaset pods status: [3/3]"
echo "All mongo pods are up and running."

echo "Applying init scripts if required."
kubectl cp -n ${K8S_CLUSTER_NAMESPACE} output/init.js harness-mongodb-replicaset-0:/tmp/init.js
kubectl exec -n ${K8S_CLUSTER_NAMESPACE} harness-mongodb-replicaset-0 -it -- bash -c "mongo mongodb://$(rv services.mongo.adminUser):$(rv services.mongo.adminPassword)@localhost/harness?replicaSet=rs0\&authSource=admin < /tmp/init.js"

echo "######################### Mongo End ##############################"
