#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

CURRENT_DIR=$PWD
PROPERTY_FILE=$PWD/installer.properties
VERSION_FILE=$PWD/version.properties
DELEGATE_VERSION=`cat $VERSION_FILE | grep "DELEGATE_VERSION" | cut -d '=' -f2`
WATCHER_VERSION=`cat $VERSION_FILE | grep "WATCHER_VERSION" | cut -d '=' -f2`
ROOT_FOLDER=`cat $PROPERTY_FILE | grep "ROOT_PATH" | cut -d '=' -f2`
UI_FOLDER=`cat $PROPERTY_FILE | grep "UI_PATH" | cut -d '=' -f2`

cd $CURRENT_DIR
echo "Started Copying Files........." 
cp -r $ROOT_FOLDER/360-cg-manager/config.yml $ROOT_FOLDER/360-cg-manager/target/rest-capsule.jar $CURRENT_DIR/
cp -r $ROOT_FOLDER/270-verification/verification-config.yml $ROOT_FOLDER/270-verification/target/verification-capsule.jar $CURRENT_DIR/
cp -r $ROOT_FOLDER/260-delegate/config-delegate.yml $ROOT_FOLDER/260-delegate/target/delegate-capsule.jar $CURRENT_DIR/
cp -r $ROOT_FOLDER/960-watcher/config-watcher.yml $ROOT_FOLDER/960-watcher/target/watcher-capsule.jar $CURRENT_DIR/
cp -r $UI_FOLDER/static $CURRENT_DIR/
echo "Copying Files completed........."

echo "preparing delegate..............."
sh prepare_delegate.sh $DELEGATE_VERSION
echo "delegate preparation finished for version : $DELEGATE_VERSION "

echo "prepating watcher................"
sh prepare_watcher.sh $WATCHER_VERSION
echo "watcher prepation completed for version : $WATCHER_VERSION "

echo "Started generating installer............"
sh generate_installer.sh
echo "Installer generation successfull"
