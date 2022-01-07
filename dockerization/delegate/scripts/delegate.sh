#!/bin/bash -e
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

function jar_app_version() {
  JAR=$1
  if unzip -l $JAR | grep -q io/harness/versionInfo.yaml
  then
    VERSION=$(unzip -c $JAR io/harness/versionInfo.yaml | grep "^version " | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  fi

  if [ -z "$VERSION" ]
  then
    if unzip -l $JAR | grep -q main/resources-filtered/versionInfo.yaml
    then
      VERSION=$(unzip -c $JAR main/resources-filtered/versionInfo.yaml | grep "^version " | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    fi
  fi

  if [ -z "$VERSION" ]
  then
    VERSION=$(unzip -c $JAR META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  fi
  echo $VERSION
}

USE_CDN="${USE_CDN:-false}"
JVM_URL_BASE_PATH=$DELEGATE_STORAGE_URL
ALPN_BOOT_JAR_BASE_PATH=$DELEGATE_STORAGE_URL
if [ "$USE_CDN" = true ]; then
  JVM_URL_BASE_PATH=$JVM_URL_BASE_PATH/public/shared
  ALPN_BOOT_JAR_BASE_PATH=$JVM_URL_BASE_PATH/public/shared
fi

if [ "$JRE_VERSION" != "" ] && [ "$JRE_VERSION" != "1.8.0_242" ]; then
  echo Unsupported JRE version $JRE_VERSION - using 1.8.0_242 instead
fi

JRE_DIR=jdk8u242-b08-jre
JVM_URL=$JVM_URL_BASE_PATH/jre/openjdk-8u242/jre_x64_linux_8u242b08.tar.gz

JRE_BINARY=$JRE_DIR/bin/java

ALPN_BOOT_JAR_URL=$ALPN_BOOT_JAR_BASE_PATH/tools/alpn/release/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

if [ -z "$1" ]; then
  echo "This script is not meant to be executed directly. The watcher uses it to manage delegate processes."
  exit 0
fi

if [ -e proxy.config ]; then
  source proxy.config
  if [[ $PROXY_HOST != "" ]]; then
    echo "Using $PROXY_SCHEME proxy $PROXY_HOST:$PROXY_PORT"
    if [[ $PROXY_USER != "" ]]; then
      export PROXY_USER
      export PROXY_PASSWORD
      export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_USER:$PROXY_PASSWORD@$PROXY_HOST:$PROXY_PORT
    else
      export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_HOST:$PROXY_PORT
      export http_proxy=$PROXY_SCHEME://$PROXY_HOST:$PROXY_PORT
      export https_proxy=$PROXY_SCHEME://$PROXY_HOST:$PROXY_PORT
    fi
    PROXY_SYS_PROPS="-DproxyScheme=$PROXY_SCHEME -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
  fi

  if [[ $PROXY_MANAGER == "true" || $PROXY_MANAGER == "" ]]; then
    export MANAGER_PROXY_CURL=$PROXY_CURL
  else
    HOST_AND_PORT_ARRAY=(${MANAGER_HOST_AND_PORT//:/ })
    MANAGER_HOST="${HOST_AND_PORT_ARRAY[1]}"
    MANAGER_HOST="${MANAGER_HOST:2}"
    echo "No proxy for Harness manager at $MANAGER_HOST"
    if [[ $NO_PROXY == "" ]]; then
      NO_PROXY=$MANAGER_HOST
    else
      NO_PROXY="$NO_PROXY,$MANAGER_HOST"
    fi
  fi

  if [[ $NO_PROXY != "" ]]; then
    echo "No proxy for domain suffixes $NO_PROXY"
    export no_proxy=$NO_PROXY
    SYSTEM_PROPERTY_NO_PROXY=`echo $NO_PROXY | sed "s/\,/|*/g"`
    PROXY_SYS_PROPS=$PROXY_SYS_PROPS" -Dhttp.nonProxyHosts=*$SYSTEM_PROPERTY_NO_PROXY"
  fi
fi

if [ ! -d $JRE_DIR -o ! -e $JRE_BINARY ]; then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl $MANAGER_PROXY_CURL -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  rm -rf $JRE_DIR
  tar xzf $JVM_TAR_FILENAME
  rm -f $JVM_TAR_FILENAME
fi

if [[ $DEPLOY_MODE != "KUBERNETES" ]]; then
  echo "Checking Delegate latest version..."
  REMOTE_DELEGATE_LATEST=$(curl $MANAGER_PROXY_CURL -ks $DELEGATE_STORAGE_URL/$DELEGATE_CHECK_LOCATION)
  REMOTE_DELEGATE_URL=$DELEGATE_STORAGE_URL/$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f2)
  REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f1)

  if [ ! -e delegate.jar ]; then
    echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
    curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    DELEGATE_CURRENT_VERSION=$(jar_app_version delegate.jar)
    if [[ $REMOTE_DELEGATE_VERSION != $DELEGATE_CURRENT_VERSION ]]; then
      echo "The current version $DELEGATE_CURRENT_VERSION is not the same as the expected remote version $REMOTE_DELEGATE_VERSION"
      echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
      mkdir -p backup.$DELEGATE_CURRENT_VERSION
      cp delegate.jar backup.$DELEGATE_CURRENT_VERSION
      curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
    fi
  fi
fi

if [ ! -e config-delegate.yml ]; then
  echo "accountId: $ACCOUNT_ID" > config-delegate.yml
  echo "accountSecret: $ACCOUNT_SECRET" >> config-delegate.yml
fi
test "$(tail -c 1 config-delegate.yml)" && `echo "" >> config-delegate.yml`
if ! `grep managerUrl config-delegate.yml > /dev/null`; then
  echo "managerUrl: $MANAGER_HOST_AND_PORT/api/" >> config-delegate.yml
fi
if ! `grep verificationServiceUrl config-delegate.yml > /dev/null`; then
  echo "verificationServiceUrl: $MANAGER_HOST_AND_PORT/verification/" >> config-delegate.yml
fi
if ! `grep cvNextGenUrl config-delegate.yml > /dev/null`; then
  echo "cvNextGenUrl: $MANAGER_HOST_AND_PORT/cv/api/" >> config-delegate.yml
fi
if ! `grep watcherCheckLocation config-delegate.yml > /dev/null`; then
  echo "watcherCheckLocation: $WATCHER_STORAGE_URL/$WATCHER_CHECK_LOCATION" >> config-delegate.yml
else
  sed -i.bak "s|^watcherCheckLocation: .*$|watcherCheckLocation: $WATCHER_STORAGE_URL/$WATCHER_CHECK_LOCATION|" config-delegate.yml
fi
if ! `grep heartbeatIntervalMs config-delegate.yml > /dev/null`; then
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
fi
if ! `grep doUpgrade config-delegate.yml > /dev/null`; then
  echo "doUpgrade: true" >> config-delegate.yml
fi
if ! `grep localDiskPath config-delegate.yml > /dev/null`; then
  echo "localDiskPath: /tmp" >> config-delegate.yml
fi
if ! `grep maxCachedArtifacts config-delegate.yml > /dev/null`; then
  echo "maxCachedArtifacts: 2" >> config-delegate.yml
fi
if ! `grep pollForTasks config-delegate.yml > /dev/null`; then
  if [ "$DEPLOY_MODE" == "ONPREM" ]; then
      echo "pollForTasks: true" >> config-delegate.yml
  else
      echo "pollForTasks: ${POLL_FOR_TASKS:-false}" >> config-delegate.yml
  fi
fi

if ! `grep useCdn config-delegate.yml > /dev/null`; then
  echo "useCdn: $USE_CDN" >> config-delegate.yml
else
  sed -i.bak "s|^useCdn:.*$|useCdn: $USE_CDN|" config-delegate.yml
fi

if ! `grep cdnUrl config-delegate.yml > /dev/null`; then
  echo "cdnUrl: $CDN_URL" >> config-delegate.yml
else
  sed -i.bak "s|^cdnUrl:.*$|cdnUrl: $CDN_URL|" config-delegate.yml
fi

if [ ! -z "$HELM3_PATH" ] && ! `grep helm3Path config-delegate.yml > /dev/null` ; then
  echo "helm3Path: $HELM3_PATH" >> config-delegate.yml
fi

if [ ! -z "$HELM_PATH" ] && ! `grep helmPath config-delegate.yml > /dev/null` ; then
  echo "helmPath: $HELM_PATH" >> config-delegate.yml
fi

if [ ! -z "$CF_CLI6_PATH" ] && ! `grep cfCli6Path config-delegate.yml > /dev/null` ; then
  echo "cfCli6Path: $CF_CLI6_PATH" >> config-delegate.yml
fi

if [ ! -z "$CF_CLI7_PATH" ] && ! `grep cfCli7Path config-delegate.yml > /dev/null` ; then
  echo "cfCli7Path: $CF_CLI7_PATH" >> config-delegate.yml
fi

if [ ! -z "$KUSTOMIZE_PATH" ] && ! `grep kustomizePath config-delegate.yml > /dev/null` ; then
  echo "kustomizePath: $KUSTOMIZE_PATH" >> config-delegate.yml
fi

if [ ! -z "$GRPC_SERVICE_ENABLED" ] && ! `grep grpcServiceEnabled config-delegate.yml > /dev/null` ; then
  echo "grpcServiceEnabled: $GRPC_SERVICE_ENABLED" >> config-delegate.yml
fi

if [ ! -z "$GRPC_SERVICE_CONNECTOR_PORT" ] && ! `grep grpcServiceConnectorPort config-delegate.yml > /dev/null` ; then
  echo "grpcServiceConnectorPort: $GRPC_SERVICE_CONNECTOR_PORT" >> config-delegate.yml
fi

rm -f -- *.bak

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [ ! -e alpn-boot-8.1.13.v20181017.jar ]; then
  curl $MANAGER_PROXY_CURL -ks $ALPN_BOOT_JAR_URL -o alpn-boot-8.1.13.v20181017.jar
fi

if [[ $DEPLOY_MODE != "KUBERNETES" ]]; then
  echo "Starting delegate - version $REMOTE_DELEGATE_VERSION"
  $JRE_BINARY $JAVA_OPTS $PROXY_SYS_PROPS -Xbootclasspath/p:alpn-boot-8.1.13.v20181017.jar $OVERRIDE_TMP_PROPS -DACCOUNT_ID="${accountId}" -DMANAGER_HOST_AND_PORT="${MANAGER_HOST_AND_PORT}" -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -Dcom.sun.jndi.ldap.object.disableEndpointIdentification=true -DLANG=en_US.UTF-8 -jar delegate.jar config-delegate.yml watched $1
fi

sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`; then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail -n 30 delegate.log)"
fi
