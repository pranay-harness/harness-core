#!/usr/bin/env bash
if [[ -v "WATCHER_JAR_URL" ]]; then
    sed -i "s|_watcherJarUrl_|${WATCHER_JAR_URL}|" /opt/harness-delegate/start.sh
fi
if [[ -v "WATCHER_UPGRADE_VERSION" ]]; then
    sed -i "s|_watcherUpgradeVersion_|${WATCHER_UPGRADE_VERSION}|" /opt/harness-delegate/start.sh
fi
if [[ -v "WATCHER_CHECK_LOCATION" ]]; then
    sed -i "s|_watcherCheckLocation_|${WATCHER_CHECK_LOCATION}|" /opt/harness-delegate/start.sh
fi
if [[ -v "DELEGATE_JAR_URL" ]]; then
    sed -i "s|_delegateJarUrl_|${DELEGATE_JAR_URL}|" /opt/harness-delegate/start.sh
    sed -i "s|_delegateJarUrl_|${DELEGATE_JAR_URL}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "DELEGATE_UPGRADE_VERSION" ]]; then
    sed -i "s|_upgradeVersion_|${DELEGATE_UPGRADE_VERSION}|" /opt/harness-delegate/start.sh
    sed -i "s|_upgradeVersion_|${DELEGATE_UPGRADE_VERSION}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "DEPLOY_MODE" ]]; then
    sed -i "s|_deployMode_|${DEPLOY_MODE}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "ACCOUNT_ID" ]]; then
    sed -i "s|_accountId_|${ACCOUNT_ID}|" /opt/harness-delegate/start.sh
    sed -i "s|_accountId_|${ACCOUNT_ID}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "ACCOUNT_SECRET" ]]; then
    sed -i "s|_accountSecret_|${ACCOUNT_SECRET}|" /opt/harness-delegate/delegate.sh
fi
if [[ -v "MANAGER_HOST_AND_PORT" ]]; then
    sed -i "s|_managerHostAndPort_|${MANAGER_HOST_AND_PORT}|" /opt/harness-delegate/delegate.sh
fi
