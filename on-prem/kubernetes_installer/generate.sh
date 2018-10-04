#!/usr/bin/env bash

set -e

source harness/scripts/utils.sh

VERSION_PROPERTY_FILE=version.yaml

echo "# Reading versions from $VERSION_PROPERTY_FILE"
echo "# Reading versions from $VERSION_PROPERTY_FILE"
leimage=$(yq r version.yaml images.le)
managerimage=$(yq r version.yaml images.manager)
mongoimage=$(yq r version.yaml images.mongo)
nginximage=$(yq r version.yaml images.nginx)
uiimage=$(yq r version.yaml images.ui)
defaultbackendimage=$(yq r version.yaml images.defaultBackend)
ingresscontrollerimage=$(yq r version.yaml images.ingressController)

INSTALLER_DIR=harness-kubernetes
ARTIFACT_DIR=$INSTALLER_DIR/artifacts
SCRIPTS_DIR=$INSTALLER_DIR/scripts
JREVERSION=8u131
SOLARIS_JRE=jre-8u131-solaris-x64.tar.gz
MACOS_JRE=jre-8u131-macosx-x64.tar.gz
LINUX_JRE=jre-8u131-linux-x64.tar.gz

echo "#######Version details start #############"
cat $VERSION_PROPERTY_FILE
printf "\n"
echo "#######Version details end #############"
printf "\n"


docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}

function downloadImages(){
   saveImage $uiimage $ARTIFACT_DIR/ui.tar
   saveImage $managerimage $ARTIFACT_DIR/manager.tar
   saveImage $leimage $ARTIFACT_DIR/le.tar
   saveImage $mongoimage $ARTIFACT_DIR/mongo.tar
   saveImage $nginximage $ARTIFACT_DIR/nginx.tar
   saveImage $ingresscontrollerimage $ARTIFACT_DIR/ingress.tar
   saveImage $defaultbackendimage $ARTIFACT_DIR/defaultbackend.tar


   curl https://app.harness.io/storage/wingsdelegates/jre/$JREVERSION/$SOLARIS_JRE > $ARTIFACT_DIR/$SOLARIS_JRE
   curl https://app.harness.io/storage/wingsdelegates/jre/$JREVERSION/$MACOS_JRE > $ARTIFACT_DIR/$MACOS_JRE
   curl https://app.harness.io/storage/wingsdelegates/jre/$JREVERSION/$LINUX_JRE > $ARTIFACT_DIR/$LINUX_JRE
   cp delegate.jar $ARTIFACT_DIR/
   cp watcher.jar $ARTIFACT_DIR/

}
managerversion=$(echo $managerimage | awk -F ":" '{print $2}')
echo "Manager version is $managerversion"

function prepareInstaller(){
    rm -rf $INSTALLER_DIR
    mkdir -p $ARTIFACT_DIR
    cp -r harness/* $INSTALLER_DIR
    chmod +x $INSTALLER_DIR/*.sh
    replace MANAGER_VERSION $managerversion "$SCRIPTS_DIR/init_delegate.sh"
    downloadImages
    yq m -i $INSTALLER_DIR/values.yaml version.yaml

}

prepareInstaller
