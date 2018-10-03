#!/usr/bin/env bash

set -e

./prepare_installer.sh
./setup_creds_namespace.sh

./init_ingress_controller.sh

./init_mongo.sh
mongo_status=$?
if [ $mongo_status -eq 1 ];then
	"Mongo was not installed successfully, exiting now "
	exit 1
fi
./init_harness_ms.sh

harness_ms_status=$?
if [ $harness_ms_status -eq 1 ];then
	"Harness Microservices did not install successfully, exiting now "
	exit 1
fi

./init_delegate.sh

init_delegate_status=$?
if [ $init_delegate_status -eq 1 ];then
	"Harness delegates were not installed successfully, exiting now "
	exit 1
fi
