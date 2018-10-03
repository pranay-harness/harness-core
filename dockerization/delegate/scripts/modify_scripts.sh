#!/usr/bin/env bash

if [[ -v "ACCOUNT_ID" ]]; then
    sed -i "s|_accountId_|${ACCOUNT_ID}|" /opt/harness-delegate/start.sh
    sed -i "s|_accountId_|${ACCOUNT_ID}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "ACCOUNT_SECRET" ]]; then
    sed -i "s|_accountSecret_|${ACCOUNT_SECRET}|" /opt/harness-delegate/start.sh
    sed -i "s|_accountSecret_|${ACCOUNT_SECRET}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "MANAGER_HOST_AND_PORT" ]]
then
    sed -i "s|_managerHostAndPort_|${MANAGER_HOST_AND_PORT}|" /opt/harness-delegate/start.sh
    sed -i "s|_managerHostAndPort_|${MANAGER_HOST_AND_PORT}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "VERIFICATION_HOST_AND_PORT" ]]
then
    sed -i "s|_verificationHostAndPort_|${VERIFICATION_HOST_AND_PORT}|" /opt/harness-delegate/start.sh
    sed -i "s|_verificationHostAndPort_|${VERIFICATION_HOST_AND_PORT}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "WATCHER_STORAGE_URL" ]]
then
    sed -i "s|_watcherStorageUrl_|${WATCHER_STORAGE_URL}|" /opt/harness-delegate/start.sh
    sed -i "s|_watcherStorageUrl_|${WATCHER_STORAGE_URL}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "WATCHER_CHECK_LOCATION" ]]
then
    sed -i "s|_watcherCheckLocation_|${WATCHER_CHECK_LOCATION}|" /opt/harness-delegate/start.sh
    sed -i "s|_watcherCheckLocation_|${WATCHER_CHECK_LOCATION}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "DELEGATE_STORAGE_URL" ]]
then
    sed -i "s|_delegateStorageUrl_|${DELEGATE_STORAGE_URL}|" /opt/harness-delegate/start.sh
    sed -i "s|_delegateStorageUrl_|${DELEGATE_STORAGE_URL}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "DELEGATE_CHECK_LOCATION" ]]
then
    sed -i "s|_delegateCheckLocation_|${DELEGATE_CHECK_LOCATION}|" /opt/harness-delegate/start.sh
    sed -i "s|_delegateCheckLocation_|${DELEGATE_CHECK_LOCATION}|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "DESCRIPTION" ]]
then
    sed -i "s|_description_|${DESCRIPTION}|" /opt/harness-delegate/delegate.sh
else
    sed -i "s|_description_|description here|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "PROXY_HOST" ]]
then
    sed -i "s|_proxyHost_|${PROXY_HOST}|" /opt/harness-delegate/start.sh
else
    sed -i "s|_proxyHost_||" /opt/harness-delegate/start.sh
fi

if [[ -v "PROXY_PORT" ]]
then
    sed -i "s|_proxyPort_|${PROXY_PORT}|" /opt/harness-delegate/start.sh
else
    sed -i "s|_proxyPort_||" /opt/harness-delegate/start.sh
fi

if [[ -v "PROXY_SCHEME" ]]
then
    sed -i "s|_proxyScheme_|${PROXY_SCHEME}|" /opt/harness-delegate/start.sh
else
    sed -i "s|_proxyScheme_||" /opt/harness-delegate/start.sh
fi

if [[ -v "NO_PROXY" ]]
then
    sed -i "s|_noProxy_|${NO_PROXY}|" /opt/harness-delegate/start.sh
else
    sed -i "s|_noProxy_||" /opt/harness-delegate/start.sh
fi

if [[ -v "DEPLOY_MODE" ]]
then
    sed -i "s|_deployMode_|${DEPLOY_MODE}|" /opt/harness-delegate/start.sh
    sed -i "s|_deployMode_|${DEPLOY_MODE}|" /opt/harness-delegate/delegate.sh
else
    sed -i "s|_deployMode_|AWS|" /opt/harness-delegate/start.sh
    sed -i "s|_deployMode_|AWS|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "POLL_FOR_TASKS" ]]
then
    sed -i "s|_pollForTasks_|${POLL_FOR_TASKS}|" /opt/harness-delegate/delegate.sh
else
    sed -i "s|_pollForTasks_|false|" /opt/harness-delegate/delegate.sh
fi

if [[ -v "HELM_DESIRED_VERSION" ]]
then
    sed -i "s|_helmVersion_|${HELM_DESIRED_VERSION}|" /opt/harness-delegate/start.sh
else
    sed -i "s|_helmVersion_|latest|" /opt/harness-delegate/start.sh
fi
