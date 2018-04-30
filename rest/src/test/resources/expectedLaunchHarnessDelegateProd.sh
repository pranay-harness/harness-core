#!/bin/bash -e

sudo docker pull harness/delegate:latest

sudo docker run -d --restart unless-stopped --hostname=$(hostname -f) \
-e ACCOUNT_ID=ACCOUNT_ID \
-e ACCOUNT_SECRET=ACCOUNT_KEY \
-e DESCRIPTION="description here" \
-e PROXY_HOST= \
-e PROXY_PORT= \
-e PROXY_SCHEME= \
-e NO_PROXY= \
-e POLL_FOR_TASKS=false \
harness/delegate:latest
