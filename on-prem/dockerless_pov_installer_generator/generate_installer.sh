#!/usr/bin/env bash

source ./utils.sh

INSTALLER_DIR=harness_installer
MANAGER_DIR=$INSTALLER_DIR/manager
VERIFICATION_DIR=$INSTALLER_DIR/verification
UI_DIR=$INSTALLER_DIR/ui
LE_DIR=$INSTALLER_DIR/le
STORAGE_DIR=$INSTALLER_DIR/storage
jre_version=8u131

rm -rf $INSTALLER_DIR

mkdir -p $INSTALLER_DIR

mkdir $MANAGER_DIR
mkdir $VERIFICATION_DIR
mkdir $UI_DIR
mkdir $STORAGE_DIR
mkdir $LE_DIR


cp -R scripts $INSTALLER_DIR/
cp -R config $INSTALLER_DIR/
cp *.properties $INSTALLER_DIR
cp install_harness.sh $INSTALLER_DIR
cp utils.sh $INSTALLER_DIR
chmod +x $INSTALLER_DIR/*.sh
CONFIG_PROPERTIES_FILE=$INSTALLER_DIR/config.properties
cp -R splunk_pyml $LE_DIR/

if [[ -z $1 ]]; then
   echo "No license file supplied, skipping setting the license file in the installer"
else
   echo "License file supplied, generating installer with license file $1"
   replace harness_license $1 ${CONFIG_PROPERTIES_FILE} $INSTALLER_DIR
fi

JRE_SOURCE_URL=https://app.harness.io/storage/wingsdelegates/jre/8u131
JRE_SOLARIS=jre-8u131-solaris-x64.tar.gz
JRE_MACOSX=jre-8u131-macosx-x64.tar.gz
JRE_LINUX=jre-8u131-linux-x64.tar.gz
JRE_LINUX_DIR=jre1.8.0_131/

curl "${JRE_SOURCE_URL}/${JRE_SOLARIS}" > "${JRE_SOLARIS}"
curl "${JRE_SOURCE_URL}/${JRE_MACOSX}" > "${JRE_MACOSX}"
curl "${JRE_SOURCE_URL}/${JRE_LINUX}" > "${JRE_LINUX}"

function setupDelegateJars(){
   echo "################################Setting up Delegate Jars ################################"

    DELEGATE_VERSION=$(getProperty "version.properties" "DELEGATE_VERSION")
    WATCHER_VERSION=$(getProperty "version.properties" "WATCHER_VERSION")

    mkdir -p $STORAGE_DIR/wingsdelegates/jre/${jre_version}/
    cp jre-8u131-solaris-x64.tar.gz $STORAGE_DIR/wingsdelegates/jre/${jre_version}/
    cp jre-8u131-macosx-x64.tar.gz $STORAGE_DIR/wingsdelegates/jre/${jre_version}/
    cp jre-8u131-linux-x64.tar.gz $STORAGE_DIR/wingsdelegates/jre/${jre_version}/

    rm -rf ${STORAGE_DIR}/wingsdelegates/jobs/deploy-prod-delegate/*
    mkdir -p  ${STORAGE_DIR}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}
    cp delegate.jar ${STORAGE_DIR}/wingsdelegates/jobs/deploy-prod-delegate/${DELEGATE_VERSION}/

    echo "1.0.${DELEGATE_VERSION} jobs/deploy-prod-delegate/${DELEGATE_VERSION}/delegate.jar" > delegateprod.txt

    mv delegateprod.txt ${STORAGE_DIR}/wingsdelegates

    rm -rf ${STORAGE_DIR}/wingswatchers/jobs/deploy-prod-watcher/*
    mkdir -p  ${STORAGE_DIR}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}
    cp watcher.jar ${STORAGE_DIR}/wingswatchers/jobs/deploy-prod-watcher/${WATCHER_VERSION}/
    echo "1.0.${WATCHER_VERSION} jobs/deploy-prod-watcher/${WATCHER_VERSION}/watcher.jar" > watcherprod.txt
    mv watcherprod.txt ${STORAGE_DIR}/wingswatchers

}


function setUpArtifacts(){
   echo "################################ Preparing artifacts in their respective folders  ################################"
   cp rest-capsule.jar $MANAGER_DIR
   cp config.yml $MANAGER_DIR

   cp verification-capsule.jar $VERIFICATION_DIR
   cp verification-config.yml $VERIFICATION_DIR

   cp -R static $UI_DIR/

   setupDelegateJars
}

function setUpJRE(){
    cp $JRE_LINUX $INSTALLER_DIR
    cd $INSTALLER_DIR
    tar -xvf $JRE_LINUX
    rm -f $JRE_LINUX
    ln -s $JRE_LINUX_DIR jre
    cd -
}


setUpJRE
setUpArtifacts
tar -cvzf harness_installer.tar.gz $INSTALLER_DIR