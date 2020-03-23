#!/bin/bash -e

sudo docker pull harness/delegate:latest

sudo docker run -d --restart unless-stopped --hostname=$(hostname -f) \
-e ACCOUNT_ID=ACCOUNT_ID \
-e ACCOUNT_SECRET=ACCOUNT_KEY \
-e MANAGER_HOST_AND_PORT=https://localhost:9090 \
-e WATCHER_STORAGE_URL=http://localhost:8888 \
-e WATCHER_CHECK_LOCATION=watcherci.txt \
-e REMOTE_WATCHER_URL_CDN=http://localhost:9500/builds/9/watcher.jar \
-e DELEGATE_STORAGE_URL=http://localhost:8888 \
-e DELEGATE_CHECK_LOCATION=delegateci.txt \
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
-e JRE_VERSION=1.8.0_191 \
harness/delegate:latest
