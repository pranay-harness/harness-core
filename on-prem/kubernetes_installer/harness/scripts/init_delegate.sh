#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
K8S_CLUSTER_NAMESPACE=$(yq r ${SCRIPT_DIR}/../values.internal.yaml kubernetesClusterNamespace)

harness_manager_nginx_status=$(kubectl get pods -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -o jsonpath="{.status.phase}")

wait=0
while [ $harness_manager_nginx_status != "Running"  ] && [ $wait -lt 30 ]
do
	sleep 3
    wait=$(($wait+1))
	echo "Waiting for pods to come up, harness_manager_nginx_status=$harness_manager_nginx_status"
	harness_manager_nginx_status=$(kubectl get pods -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -o jsonpath="{.status.phase}")
done

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- rm -rf /www/data/storage/wingswatchers/

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- rm -rf /www/data/storage/wingsdelegates/

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- mkdir -p /www/data/storage/wingswatchers/

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- mkdir -p /www/data/storage/wingsdelegates/

echo "Created the directories on the harness-manager-nginx pod"

echo "1.0.MANAGER_VERSION jobs/deploy-prod-watcher/MANAGER_VERSION/watcher.jar" > artifacts/watcherprod.txt

echo "1.0.MANAGER_VERSION jobs/deploy-prod-delegate/MANAGER_VERSION/delegate.jar" > artifacts/delegateprod.txt

kubectl cp artifacts/watcherprod.txt harness/harness-manager-nginx-0:/www/data/storage/wingswatchers/watcherprod.txt

kubectl cp artifacts/delegateprod.txt harness/harness-manager-nginx-0:/www/data/storage/wingsdelegates/delegateprod.txt

echo "Copied over the metadata file over to harness-manager-nginx-0" 

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- mkdir -p  /www/data/storage/wingswatchers/jobs/deploy-prod-watcher/MANAGER_VERSION

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- mkdir -p  /www/data/storage/wingsdelegates/jobs/deploy-prod-delegate/MANAGER_VERSION

kubectl cp artifacts/watcher.jar harness/harness-manager-nginx-0:/www/data/storage/wingswatchers/jobs/deploy-prod-watcher/MANAGER_VERSION/

kubectl cp artifacts/delegate.jar harness/harness-manager-nginx-0:/www/data/storage/wingsdelegates/jobs/deploy-prod-delegate/MANAGER_VERSION/

echo "Copied over the watcher and delegate jars over to harness-manager-nginx-0" 

kubectl exec -it -n ${K8S_CLUSTER_NAMESPACE} harness-manager-nginx-0 -- mkdir -p /www/data/storage/wingsdelegates/jre/8u131/

kubectl cp artifacts/jre-8u131-solaris-x64.tar.gz harness/harness-manager-nginx-0:/www/data/storage/wingsdelegates/jre/8u131/

kubectl cp artifacts/jre-8u131-linux-x64.tar.gz harness/harness-manager-nginx-0:/www/data/storage/wingsdelegates/jre/8u131/

kubectl cp artifacts/jre-8u131-macosx-x64.tar.gz harness/harness-manager-nginx-0:/www/data/storage/wingsdelegates/jre/8u131/

echo "Copied over the jre files over to harness-manager-nginx-0" 
