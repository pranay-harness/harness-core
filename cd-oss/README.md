# Run Harness Locally with Docker
This page contains the instructions necessary to run Harness using Docker Compose.

## Pre-requisites
1) Install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) on your system

## Hardware requirements
* 4 CPUs or more
* 12GB of free memory
* 20GB of free disk space
* Internet connection

For Docker Desktop, see [Docker for Mac](https://docs.docker.com/docker-for-mac/#resources) or [Docker for Windows](https://docs.docker.com/docker-for-windows/#resources) for details on increasing resources

## Starting Harness
1) Clone this repo
   ```shell
   git clone git@github.com:wings-software/portal.git
   cd portal/
   ```
1) Login to docker  
   Retrieve password from [Vault](https://vault-internal.harness.io:8200/ui/vault/secrets/secret/show/credentials/dockerdev) then run
   ```shell
   docker login -u harnessdev
   ```
1) Build the application and docker images
   ```shell
   ./cd-oss/cd-oss.sh
   ```
1) Set the `PUBLIC_URL` environment variable, this is the IP address of your machine  
   Example,
   ```shell
   export PUBLIC_URL="http://192.168.0.1"
   ```
1) Start harness
   ```shell
   docker-compose -f cd-oss/docker-compose.yml up -d
   ```

## Using Harness
1) To get the link to your instance of Harness run
   ```shell
   echo "http://${PUBLIC_URL}/#/onprem-signup"
   ```
1) Open the printed link and complete the signup form
1) After registration, you will be redirected to the login screen where you can login and begin using Harness
1) Follow the [documentation](https://docs.harness.io/category/3err8eu6x3-account) for using Harness

## Remove Harness
```shell
docker-compose -f cd-oss/docker-compose.yml down
```

## Upgrading an existing Harness Installation
1) Update this repository
   ```shell
   git pull
   ```
2) Re-run docker compose
   ```shell
   docker-compose -f cd-oss/docker-compose.yml up -d
   ```
