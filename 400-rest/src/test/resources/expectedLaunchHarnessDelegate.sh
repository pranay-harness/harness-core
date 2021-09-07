#!/bin/bash -e

sudo docker pull harness/delegate:latest

sudo docker run -d --restart unless-stopped --hostname=$(hostname -f | head -c 63) \
-e ACCOUNT_ID=ACCOUNT_ID \
-e ACCOUNT_SECRET=TOKEN_VALUE \
-e MANAGER_HOST_AND_PORT=https://localhost:9090 \
-e WATCHER_STORAGE_URL=http://localhost:8888 \
-e WATCHER_CHECK_LOCATION=watcherci.txt \
-e REMOTE_WATCHER_URL_CDN=http://localhost:9500/builds \
-e DELEGATE_STORAGE_URL=http://localhost:8888 \
-e DELEGATE_CHECK_LOCATION=delegateci.txt \
-e DELEGATE_NAME=harness-delegate \
-e DELEGATE_PROFILE=QFWin33JRlKWKBzpzE5A9A \
-e DELEGATE_TYPE=DOCKER \
-e DEPLOY_MODE=KUBERNETES \
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
-e USE_CDN=false \
-e CDN_URL=http://localhost:9500 \
-e JRE_VERSION=1.8.0_242 \
-e HELM3_PATH= \
-e HELM_PATH= \
-e CF_CLI6_PATH= \
-e CF_CLI7_PATH= \
-e KUSTOMIZE_PATH= \
-e OC_PATH= \
-e KUBECTL_PATH= \
-e CLIENT_TOOLS_DOWNLOAD_DISABLED=false \
-e INSTALL_CLIENT_TOOLS_IN_BACKGROUND=true \
harness/delegate:latest
