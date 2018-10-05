#!/usr/bin/env bash

set -e

source scripts/utils.sh

echo "Fetching kubernetes cluster information..."
K8S_CLUSTER_NAMESPACE=$(yq r values.yaml kubernetes-cluster-namespace)
echo "Kubernetes cluster namespace: $K8S_CLUSTER_NAMESPACE"
kubectl cluster-info
echo "Above kubernetes cluster and namespace will be used for harness installation"
confirm

[ ! -e values.internal.yaml ] || rm values.internal.yaml
cp values.yaml values.internal.yaml

docker_registry_username=$(yq r values.internal.yaml privatedockerrepo.docker_registry_username)
docker_registry_password=$(yq r values.internal.yaml privatedockerrepo.docker_registry_password)
docker_registry_url=$(yq r values.internal.yaml privatedockerrepo.docker_registry_url)

VERSION_PROPERTY_FILE=version.properties

if [[ $docker_registry_username == "" ]] || [[ $docker_registry_password == "" ]] || [[ $docker_registry_url == "" ]] ; then
    echo "Docker credentials incomplete"
    cat config.properties
    exit 1
fi

echo $docker_registry_password | docker login $docker_registry_url -u $docker_registry_username --password-stdin

if [[ $? -eq 1 ]]; then
    echo "Docker login failed";
    cat config.properties
    exit 1
fi

function loadDockerImages(){
    docker load --input artifacts/defaultbackend.tar
    docker load --input artifacts/ingress.tar
    docker load --input artifacts/le.tar
    docker load --input artifacts/manager.tar
    docker load --input artifacts/mongo.tar
    docker load --input artifacts/nginx.tar
    docker load --input artifacts/ui.tar
}

echo "# Reading versions from $VERSION_PROPERTY_FILE"
leimage=$(yq r values.internal.yaml images.le)
managerimage=$(yq r values.internal.yaml images.manager)
mongoimage=$(yq r values.internal.yaml images.mongo)
nginximage=$(yq r values.internal.yaml images.nginx)
uiimage=$(yq r values.internal.yaml images.ui)
defaultbackendimage=$(yq r values.internal.yaml images.defaultBackend)
ingresscontrollerimage=$(yq r values.internal.yaml images.ingressController)



echo "#######Version details start #############"
echo "leimage="$leimage
echo "managerimage="$managerimage
echo "mongoimage="$mongoimage
echo "nginximage="$nginximage
echo "uiimage="$uiimage
echo "ingresscontrollerimage="$ingresscontrollerimage
echo "defaultbackendimage="$defaultbackendimage

echo "#######Version details end #############"
printf "\n"


function prepareandUploadImage(){
    image=$1 ## images.le

    if [[ $image != $(yq r values.internal.yaml privatedockerrepo.docker_registry_url)* ]]; then
        docker tag $image $docker_registry_url/$image
        docker push $docker_registry_url/$image
    else
        docker push $image
    fi

}

function uploadDockerImages(){

    prepareandUploadImage $leimage
    prepareandUploadImage $managerimage
    prepareandUploadImage $uiimage
    prepareandUploadImage $mongoimage
    prepareandUploadImage $nginximage
    prepareandUploadImage $ingresscontrollerimage
    prepareandUploadImage $defaultbackendimage
}




loadDockerImages
uploadDockerImages



