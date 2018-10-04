#!/usr/bin/env bash

set -e

source scripts/utils.sh

docker_registry_username=$(yq r values.yaml privatedockerrepo.docker_registry_username)
docker_registry_password=$(yq r values.yaml privatedockerrepo.docker_registry_password)
docker_registry_url=$(yq r values.yaml privatedockerrepo.docker_registry_url)

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
leimage=$(yq r values.yaml images.le)
managerimage=$(yq r values.yaml images.manager)
mongoimage=$(yq r values.yaml images.mongo)
nginximage=$(yq r values.yaml images.nginx)
uiimage=$(yq r values.yaml images.ui)
defaultbackendimage=$(yq r values.yaml images.defaultBackend)
ingresscontrollerimage=$(yq r values.yaml images.ingressController)



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

    if [[ $image != $(yq r values.yaml privatedockerrepo.docker_registry_url)* ]]; then
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



