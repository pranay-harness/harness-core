#!/usr/bin/env sh

# set session
set -m

# Set Environment Variables.
export WINGS_BACKUP_PATH="/tmp/backup"
export PORT="8080"
export PASSWORD="aSecret"
export WINGS_RUNTIME_PATH="/tmp/runtime"
export WINGS_SCRIPT_DIR="/tmp/ACTIVITY_ID"
export ARTIFACT_FILE_NAME="artifact.war"
export WINGS_STAGING_PATH="/tmp/staging"

# Display Environment Variables.
echo "export PORT=\"8080\""
echo "export PASSWORD=\"*****\""

set -x

if [ "$#" -gt 1 ]
then
  key="$1"
  case $key in
    -w)
    shift # past argument
    eval WINGS_SCRIPT_WORKING_DIRECTORY="$1"
    cd "$WINGS_SCRIPT_WORKING_DIRECTORY"
    shift
    ;;
    *)
    ;;
  esac
fi

WINGS_SCRIPT_NAME="$1"
shift

$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME "$@"
