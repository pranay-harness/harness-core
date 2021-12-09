# Run Harness Locally with Docker
This page contains the instructions necessary to run Harness using Docker Compose.

## Pre-requisites
1) Install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) on your system

## Hardware requirements
* 4 CPUs or more
* 4GB of free memory
* 20GB of free disk space
* Internet connection

## Installing Harness
1) Increase Docker Desktop memory to 4GB and CPU to 4  
For Docker Desktop, see [Docker for Mac](https://docs.docker.com/docker-for-mac/#resources) or [Docker for Windows](https://docs.docker.com/docker-for-windows/#resources) for details on increasing resources
1) Clone this repo
   ```shell
   git clone git@github.com:wings-software/portal.git
   cd portal/
   ```
1) Build the application and docker images
   ```shell
   ./cd-oss/cd-oss.sh
   ```
1) Set the `PUBLIC_IP` environment variable, this is the IP address of your machine  
   Example,
   ```shell
   export PUBLIC_IP="192.168.0.1"
   ```
1) Start harness
   ```shell
   docker-compose -f cd-oss/docker-compose.yml up -d
   ```
1) Wait for startup to complete
   ```shell
   docker-compose -f cd-oss/docker-compose.yml run --rm proxy wait-for-it.sh ng-manager:7090 -t 900
   ```

## Using Harness
1) To get the link to your instance of Harness run
   ```shell
   echo "http://${PUBLIC_IP}/#/signup"
   ```
1) Open the printed link and complete the signup form
1) Follow the [documentation](https://ngdocs.harness.io/article/u8lgzsi7b3-quickstarts) for using Harness

## Stop Harness
```shell
docker-compose -f cd-oss/docker-compose.yml down
```

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
