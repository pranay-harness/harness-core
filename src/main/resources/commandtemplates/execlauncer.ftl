#!/bin/sh

set -x
# set session
set -m

# Set Environment Variables.
<#list envVariables?keys as envVariable>
${envVariable}=${envVariables.get(envVariable)}
export ${envVariable}
</#list>

<#if workingDirectory?has_content>
cd ${workingDirectory}
</#if>

$WINGS_SCRIPT_DIR/$1
