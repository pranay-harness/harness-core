#!/bin/bash -e

sudo docker pull ${delegateDockerImage}

sudo docker run -d --restart unless-stopped --hostname=$(hostname -f) \
-e ACCOUNT_ID=${accountId} \
-e ACCOUNT_SECRET=${accountSecret} \
-e MANAGER_HOST_AND_PORT=${managerHostAndPort} \
-e WATCHER_STORAGE_URL=${watcherStorageUrl} \
-e WATCHER_CHECK_LOCATION=${watcherCheckLocation} \
-e DELEGATE_STORAGE_URL=${delegateStorageUrl} \
-e DELEGATE_CHECK_LOCATION=${delegateCheckLocation} \
-e DEPLOY_MODE=${deployMode} \
-e PROXY_HOST= \
-e PROXY_PORT= \
-e PROXY_SCHEME= \
-e PROXY_USER= \
-e PROXY_PASSWORD= \
-e NO_PROXY= \
-e PROXY_MANAGER=true \
-e POLL_FOR_TASKS=false \
-e HELM_DESIRED_VERSION= \
-e CF_PLUGIN_HOME= \
-e MANAGER_TARGET=${managerTarget} \
-e MANAGER_AUTHORITY=${managerAuthority} \
<#if CCM_EVENT_COLLECTION??>
-e PUBLISH_TARGET=${publishTarget} \
-e PUBLISH_AUTHORITY=${publishAuthority} \
-e QUEUE_FILE_PATH=${queueFilePath} \
-e ENABLE_PERPETUAL_TASKS=${enablePerpetualTasks} \
</#if>
${delegateDockerImage}
