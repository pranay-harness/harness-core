#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
source ${SCRIPT_DIR}/utils.sh

docker_registry_username=$(rv docker.registry.username)
docker_registry_password=$(rv docker.registry.password)
docker_registry_url=$(rv docker.registry.url)

VERSION_PROPERTY_FILE=version.properties

if [[ $docker_registry_username == "" ]] || [[ $docker_registry_password == "" ]] || [[ $docker_registry_url == "" ]] || [[ $docker_registry_username == "null" ]] || [[ $docker_registry_password == "null" ]] || [[ $docker_registry_url == "null" ]] ; then
    echo "Docker credentials incomplete"
    cat values.yaml
    exit 1
fi

if [[ $(echo $docker_registry_password | docker login $docker_registry_url -u $docker_registry_username --password-stdin) ]]; then
    echo "Docker login successful";
else
    echo "Docker login failed";
    cat values.yaml;
    exit 1;
fi

function loadDockerImages(){
    docker load --input artifacts/defaultbackend.tar
    docker load --input artifacts/ingress.tar
    docker load --input artifacts/le.tar
    docker load --input artifacts/manager.tar
    docker load --input artifacts/verification.tar
    docker load --input artifacts/mongo.tar
    docker load --input artifacts/mongoinstall.tar
    docker load --input artifacts/nginx.tar
    docker load --input artifacts/ui.tar
    docker load --input artifacts/delegate.tar
    docker load --input artifacts/busybox.tar
}

function prepareandUploadImage(){
    image=$1 ## images.le

    if [[ $image != $(rv docker.registry.url)* ]]; then
        docker tag $image $docker_registry_url/$image
        docker push $docker_registry_url/$image
    else
        docker push $image
    fi
}

function uploadDockerImages(){
    prepareandUploadImage $leimage
    prepareandUploadImage $managerimage
    prepareandUploadImage $verificationimage
    prepareandUploadImage $uiimage
    prepareandUploadImage $mongoimage
    prepareandUploadImage $mongoinstallimage
    prepareandUploadImage $nginximage
    prepareandUploadImage $ingresscontrollerimage
    prepareandUploadImage $defaultbackendimage
    prepareandUploadImage $delegateimage
    prepareandUploadImage $busyboximage
}

echo "# Reading versions from $VERSION_PROPERTY_FILE"
leimage=$(rv images.le.repository):$(rv images.le.tag)
managerimage=$(rv images.manager.repository):$(rv images.manager.tag)
verificationimage=$(rv images.verification.repository):$(rv images.verification.tag)
mongoimage=$(rv images.mongo.repository):$(rv images.mongo.tag)
mongoinstallimage=$(rv images.mongoInstall.repository):$(rv images.mongoInstall.tag)
nginximage=$(rv images.nginx.repository):$(rv images.nginx.tag)
uiimage=$(rv images.ui.repository):$(rv images.ui.tag)
defaultbackendimage=$(rv images.defaultBackend.repository):$(rv images.defaultBackend.tag)
ingresscontrollerimage=$(rv images.ingressController.repository):$(rv images.ingressController.tag)
delegateimage=$(rv images.delegate.repository):$(rv images.delegate.tag)
busyboximage=$(rv images.busybox.repository):$(rv images.busybox.tag)


echo "#######Version details start #############"
echo "leimage="$leimage
echo "managerimage="$managerimage
echo "verificationimage="$verificationimage
echo "mongoimage="$mongoimage
echo "nginximage="$nginximage
echo "uiimage="$uiimage
echo "ingresscontrollerimage="$ingresscontrollerimage
echo "defaultbackendimage="$defaultbackendimage
echo "delegateimage="$delegateimage
echo "#######Version details end #############"
echo ""

loadDockerImages
uploadDockerImages



