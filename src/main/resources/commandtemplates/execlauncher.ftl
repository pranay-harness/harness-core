#!/bin/sh

set -x
# set session
set -m

# Set Environment Variables.
<#list envVariables?keys as envVariable>
${envVariable}=${envVariables[envVariable]}
export ${envVariable}
</#list>

$WINGS_SCRIPT_WORKING_DIRECTORY=$1
shift
$WINGS_SCRIPT_NAME=$1
shift

cd $SCRIPT_WORKING_DIRECTORY
$WINGS_SCRIPT_DIR/$SCRIPT_NAME "$@"
