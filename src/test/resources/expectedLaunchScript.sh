#!/bin/sh

set -x
# set session
set -m

# Set Environment Variables.
WINGS_BACKUP_PATH=/tmp/backup
export WINGS_BACKUP_PATH
WINGS_RUNTIME_PATH=/tmp/runtime
export WINGS_RUNTIME_PATH
WINGS_SCRIPT_DIR=/tmp/ACTIVITY_ID
export WINGS_SCRIPT_DIR
WINGS_STAGING_PATH=/tmp/staging
export WINGS_STAGING_PATH

if [  $# -gt 1 ]
then
  eval WINGS_SCRIPT_WORKING_DIRECTORY=$1
  cd $WINGS_SCRIPT_WORKING_DIRECTORY
  shift
fi
WINGS_SCRIPT_NAME=$1
shift

$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME
