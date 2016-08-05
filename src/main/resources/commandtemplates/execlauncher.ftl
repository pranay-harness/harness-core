#!/bin/bash

set -x
# set session
set -m

# Set Environment Variables.
<#list envVariables?keys as envVariable>
${envVariable}=${envVariables[envVariable]}
export ${envVariable}
</#list>

if [  $# -gt 1 ]
then
  eval WINGS_SCRIPT_WORKING_DIRECTORY=$1
  cd "$WINGS_SCRIPT_WORKING_DIRECTORY"
  shift
fi
WINGS_SCRIPT_NAME=$1
shift

$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME "$@"
