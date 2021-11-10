version: "3.7"
services:
  harness-ng-delegate:
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: "${delegateCpu}"
          memory: ${delegateXmx}M
    image: ${delegateDockerImage}
    environment:
      - ACCOUNT_ID=${accountId}
      - ACCOUNT_SECRET=${accountSecret}
      - MANAGER_HOST_AND_PORT=${managerHostAndPort}
      - WATCHER_STORAGE_URL=${watcherStorageUrl}
      - WATCHER_CHECK_LOCATION=${watcherCheckLocation}
      - REMOTE_WATCHER_URL_CDN=${remoteWatcherUrlCdn}
      - DELEGATE_STORAGE_URL=${delegateStorageUrl}
      - DELEGATE_CHECK_LOCATION=${delegateCheckLocation}
      - DEPLOY_MODE=DOCKER
      - DELEGATE_NAME=${delegateName}
      - NEXT_GEN=true
      - DELEGATE_DESCRIPTION=${delegateDescription}
      - DELEGATE_TYPE=DOCKER
      - DELEGATE_TAGS=${delegateTags}
      - DELEGATE_TASK_LIMIT=${delegateTaskLimit}
      - DELEGATE_ORG_IDENTIFIER=${delegateOrgIdentifier}
      - DELEGATE_PROJECT_IDENTIFIER=${delegateProjectIdentifier}
      - PROXY_MANAGER=true
      - USE_CDN=${useCdn}
      - CDN_URL=${cdnUrl}
      - VERSION_CHECK_DISABLED=${versionCheckDisabled}