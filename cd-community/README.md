# Harness Community Edition - Docker Compose
This page contains the instructions necessary to run Harness CE using Docker Compose.

## Pre-requisites
1) Install [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) on your system

## Hardware requirements
* 4 CPUs or more
* 4GB of free memory
* 20GB of free disk space
* Internet connection

## Installing Harness
1) For Windows and Mac, increase Docker Desktop memory to 4GB and CPU to 4  
   See [Docker for Mac](https://docs.docker.com/docker-for-mac/#resources) or [Docker for Windows](https://docs.docker.com/docker-for-windows/#resources) for details on increasing resources
1) Clone this repo
   ```shell
   git clone https://github.com/harness/harness-core.git
   cd harness-core
   ```
1) Follow the setup instructions from the root of this repo first  
   It is important that you create the `.bazelrc` file and run the `scripts/bazel/generate_credentials.sh` script before continuing.
1) Build the application and docker images
   ```shell
   ./cd-community/build.sh
   ```
1) If you are running Docker Desktop on Windows or Mac skip this step. If you are running on Linux or wish to run a production install then please see [Advanced Configuration](#advanced-configuration) to set the hostname of your machine.
1) Start harness
   ```shell
   docker-compose -f cd-community/docker-compose.yml up -d
   ```
1) Wait for startup to complete
   ```shell
   docker-compose -f cd-community/docker-compose.yml run --rm proxy wait-for-it.sh ng-manager:7090 -t 180
   ```

## Using Harness
1) Open http://localhost/#/signup
1) Complete the signup form
1) You need to install a Harness delegate before you can run pipelines, see [Install a Docker Delegate](https://ngdocs.harness.io/article/cya29w2b99-install-a-docker-delegate)
1) For help with getting started, read the Harness [documentation](https://ngdocs.harness.io/article/u8lgzsi7b3-quickstarts)

## Troubleshooting
If you run into issues when installing Harness this section will help identify where the issue is.
##### View running processes
```shell
docker-compose -f cd-community/docker-compose.yml ps
```
##### View logs of processes
```shell
docker-compose -f cd-community/docker-compose.yml logs -f <NAME>
```
Example,
```shell
docker-compose -f cd-community/docker-compose.yml logs -f manager
```

## Support
[Community forums](https://community.harness.io/)  
[Community Slack channel](https://harnesscommunity.slack.com/archives/C02K03Q5L0J)

## Stop Harness
```shell
docker-compose -f cd-community/docker-compose.yml down
```

## Advanced Configuration
### How to deploy the Harness Delegate to a separate environment
You simply need to set the `HARNESS_HOST` environment variable, see [Set hostname environment variable](#set-hostname-environment-variable) below.
### Set hostname environment variable
1) Set the `HARNESS_HOST` environment variable, this should be the IP address or hostname of the machine where you are deploying Harness. You cannot use `localhost`.  
   Example,
   ```shell
   export HARNESS_HOST="192.168.0.1"
   ```

## Remove Harness
To uninstall your instance of Harness run
```shell
docker-compose -f cd-community/docker-compose.yml down -v
```
To remove all Harness docker images from your system run
```shell
docker images --filter=reference='bazel/*/*' --quiet | xargs docker rmi
```

## Upgrade an existing installation
1) Update this repository
   ```shell
   git pull
   ```
1) Re-build
   ```shell
   ./cd-community/build.sh
   ```
1) Re-run docker compose
   ```shell
   docker-compose up -d
   ```
