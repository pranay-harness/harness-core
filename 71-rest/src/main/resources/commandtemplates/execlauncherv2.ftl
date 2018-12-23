#!/usr/bin/env sh

# set session
set -m

# Set Environment Variables.
<#list envVariables?keys as envVariable>
export ${envVariable}="${envVariables[envVariable]}"
</#list>

# Display Environment Variables.
<#list safeEnvVariables?keys as safeEnvVariable>
echo "export ${safeEnvVariable}=\"${safeEnvVariables[safeEnvVariable]}\""
</#list>

eval WINGS_SCRIPT_WORKING_DIRECTORY="${scriptWorkingDirectory}"
if [[ -n "$WINGS_SCRIPT_WORKING_DIRECTORY" ]]
then
    cd "$WINGS_SCRIPT_WORKING_DIRECTORY"
fi
