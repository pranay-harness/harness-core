#!/usr/bin/env bash
if [[ -v "WATCHER_JAR_URL" ]]; then
    sed -i "s|${watcherJarUrl}|${WATCHER_JAR_URL}|" /opt/harness-delegate/start.sh
fi
if [[ -v "WATCHER_UPGRADE_VERSION" ]]; then
    sed -i "s|${watcherUpgradeVersion}|${WATCHER_UPGRADE_VERSION}|" /opt/harness-delegate/start.sh
fi
if [[ -v "WATCHER_CHECK_LOCATION" ]]; then
    sed -i "s|${watcherCheckLocation}|${WATCHER_CHECK_LOCATION}|" /opt/harness-delegate/start.sh
fi
if [[ -v "DELEGATE_JAR_URL" ]]; then
    sed -i "s|${delegateJarUrl}|${DELEGATE_JAR_URL}|" /opt/harness-delegate/start.sh
    sed -i "s|${delegateJarUrl}|${DELEGATE_JAR_URL}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "DELEGATE_UPGRADE_VERSION" ]]; then
    sed -i "s|${upgradeVersion}|${DELEGATE_UPGRADE_VERSION}|" /opt/harness-delegate/start.sh
    sed -i "s|${upgradeVersion}|${DELEGATE_UPGRADE_VERSION}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "DEPLOY_MODE" ]]; then
    sed -i "s|${deployMode}|${DEPLOY_MODE}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "ACCOUNT_ID" ]]; then
    sed -i "s|${accountId}|${ACCOUNT_ID}|" /opt/harness-delegate/start.sh
    sed -i "s|${accountId}|${ACCOUNT_ID}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "ACCOUNT_SECRET" ]]; then
    sed -i "s|${accountSecret}|${ACCOUNT_SECRET}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "MANAGER_HOST_AND_PORT" ]]; then
    sed -i "s|${managerHostAndPort}|${MANAGER_HOST_AND_PORT}|" /opt/harness-delegate/delegate.sh
fi
