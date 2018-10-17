#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
source ${SCRIPT_DIR}/scripts/utils.sh

echo "Fetching kubernetes cluster information..."
echo ""
K8S_CLUSTER_NAMESPACE=$(yq r ${SCRIPT_DIR}/values.yaml kubernetesClusterNamespace)
echo "Kubernetes cluster namespace: $K8S_CLUSTER_NAMESPACE"
kubectl cluster-info
echo ""
echo "Above kubernetes cluster and namespace will be used for harness installation"
echo ""
confirm

yq m -x -i ${SCRIPT_DIR}/values.internal.yaml ${SCRIPT_DIR}/values.yaml

echo ""
echo "Looking for existing installation..."
if [[ $(kubectl get configmaps -n ${K8S_CLUSTER_NAMESPACE} scripts-configmap &> /dev/null) ]]; then
    echo "Existing harness installation found in given kubernetes cluster and namespace. Use upgrade.sh to upgrade. Exiting."
    echo ""
    exit 1
fi
echo "No existing installation found. Continuing with the installation."
echo ""

${SCRIPT_DIR}/scripts/upload_docker_images.sh
${SCRIPT_DIR}/scripts/install_harness_kubernetes.sh

echo ""
echo "Installation completed successfully. Let's get ship done !!!"