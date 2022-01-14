#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

${SCRIPT_DIR}/prepare_installer.sh
${SCRIPT_DIR}/setup_creds_namespace.sh

${SCRIPT_DIR}/init_ingress_controller.sh

${SCRIPT_DIR}/init_mongo.sh
mongo_status=$?
if [ $mongo_status -eq 1 ];then
	"Mongo was not installed successfully, exiting now "
	exit 1
fi

${SCRIPT_DIR}/init_harness_ms.sh

harness_ms_status=$?
if [ $harness_ms_status -eq 1 ];then
	"Harness Microservices did not install successfully, exiting now "
	exit 1
fi

${SCRIPT_DIR}/init_delegate.sh

init_delegate_status=$?
if [ $init_delegate_status -eq 1 ];then
	"Harness delegates were not installed successfully, exiting now "
	exit 1
fi
