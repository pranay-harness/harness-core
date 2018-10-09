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
-e NO_PROXY= \
-e POLL_FOR_TASKS=false \
-e HELM_DESIRED_VERSION= \
${delegateDockerImage}
