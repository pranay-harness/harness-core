#!/bin/bash -e

JRE_DIR=jre1.8.0_191
JRE_BINARY=$JRE_DIR/bin/java
JVM_URL=_delegateStorageUrl_/jre/8u191/jre-8u191-linux-x64.tar.gz

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

if [ ! -e proxy.config ]; then
  echo "PROXY_HOST=_proxyHost_" > proxy.config
  echo "PROXY_PORT=_proxyPort_" >> proxy.config
  echo "PROXY_SCHEME=_proxyScheme_" >> proxy.config
  echo "PROXY_USER=_proxyUser_" >> proxy.config
  echo "PROXY_PASSWORD=_proxyPass_" >> proxy.config
  echo "NO_PROXY=_noProxy_" >> proxy.config
  echo "PROXY_MANAGER=_proxyManager_" >> proxy.config
fi

source proxy.config
if [[ $PROXY_HOST != "" ]]; then
  echo "Using $PROXY_SCHEME proxy $PROXY_HOST:$PROXY_PORT"
  if [[ $PROXY_USER != "" ]]; then
    echo "using proxy auth config"
    export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_USER:$PROXY_PASSWORD@$PROXY_HOST:$PROXY_PORT
    PROXY_SYS_PROPS="-Dhttp.proxyUser=$PROXY_USER -Dhttp.proxyPassword=$PROXY_PASSWORD -Dhttps.proxyUser=$PROXY_USER -Dhttps.proxyPassword=$PROXY_PASSWORD "
  else
    export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_HOST:$PROXY_PORT
    export http_proxy=$PROXY_HOST:$PROXY_PORT
    export https_proxy=$PROXY_HOST:$PROXY_PORT
  fi
  PROXY_SYS_PROPS=$PROXY_SYS_PROPS" -DproxyScheme=$PROXY_SCHEME -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
fi

if [[ $PROXY_MANAGER == "true" || $PROXY_MANAGER == "" ]]; then
  export MANAGER_PROXY_CURL=$PROXY_CURL
else
  MANAGER_HOST_AND_PORT=_managerHostAndPort_
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

echo $PROXY_SYS_PROPS

ACCOUNT_STATUS=$(curl $MANAGER_PROXY_CURL -ks _managerHostAndPort_/api/account/_accountId_/status | cut -d ":" -f 3 | cut -d "," -f 1 | cut -d "\"" -f 2)
if [[ $ACCOUNT_STATUS == "DELETED" ]]; then
  rm -rf *
  touch __deleted__
  while true; do sleep 60s; done
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

if [ ! -d $JRE_DIR  -o ! -e $JRE_BINARY ]; then
  echo "No JRE available. Exiting."
  exit 1
fi

DESIRED_VERSION=_helmVersion_
if [[ $DESIRED_VERSION != "" ]]; then
  export DESIRED_VERSION
  echo "Installing Helm $DESIRED_VERSION ..."
  curl $PROXY_CURL -#k https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
  helm init --client-only
fi

echo "Checking Watcher latest version..."
WATCHER_STORAGE_URL=_watcherStorageUrl_
REMOTE_WATCHER_LATEST=$(curl $MANAGER_PROXY_CURL -ks $WATCHER_STORAGE_URL/_watcherCheckLocation_)
REMOTE_WATCHER_URL=$WATCHER_STORAGE_URL/$(echo $REMOTE_WATCHER_LATEST | cut -d " " -f2)
REMOTE_WATCHER_VERSION=$(echo $REMOTE_WATCHER_LATEST | cut -d " " -f1)

if [ ! -e watcher.jar ]; then
  echo "Downloading Watcher $REMOTE_WATCHER_VERSION ..."
  curl $MANAGER_PROXY_CURL -#k $REMOTE_WATCHER_URL -o watcher.jar
else
  WATCHER_CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_WATCHER_VERSION != $WATCHER_CURRENT_VERSION ]]; then
    echo "Downloading Watcher $REMOTE_WATCHER_VERSION ..."
    mkdir -p watcherBackup.$WATCHER_CURRENT_VERSION
    cp watcher.jar watcherBackup.$WATCHER_CURRENT_VERSION
    curl $MANAGER_PROXY_CURL -#k $REMOTE_WATCHER_URL -o watcher.jar
  fi
fi

export DEPLOY_MODE=_deployMode_

if [[ $DEPLOY_MODE != "KUBERNETES" ]]; then
  echo "Checking Delegate latest version..."
  DELEGATE_STORAGE_URL=_delegateStorageUrl_
  REMOTE_DELEGATE_LATEST=$(curl $MANAGER_PROXY_CURL -ks $DELEGATE_STORAGE_URL/_delegateCheckLocation_)
  REMOTE_DELEGATE_URL=$DELEGATE_STORAGE_URL/$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f2)
  REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f1)

  if [ ! -e delegate.jar ]; then
    echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
    curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    DELEGATE_CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    if [[ $REMOTE_DELEGATE_VERSION != $DELEGATE_CURRENT_VERSION ]]; then
      echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
      mkdir -p backup.$DELEGATE_CURRENT_VERSION
      cp delegate.jar backup.$DELEGATE_CURRENT_VERSION
      curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
    fi
  fi
fi

if [ ! -e config-watcher.yml ]; then
  echo "accountId: _accountId_" > config-watcher.yml
fi
test "$(tail -c 1 config-watcher.yml)" && `echo "" >> config-watcher.yml`
if ! `grep accountSecret config-watcher.yml > /dev/null`; then
  echo "accountSecret: _accountSecret_" >> config-watcher.yml
fi
if ! `grep managerUrl config-watcher.yml > /dev/null`; then
  echo "managerUrl: _managerHostAndPort_/api/" >> config-watcher.yml
fi
if ! `grep doUpgrade config-watcher.yml > /dev/null`; then
  echo "doUpgrade: true" >> config-watcher.yml
fi
if ! `grep upgradeCheckLocation config-watcher.yml > /dev/null`; then
  echo "upgradeCheckLocation: _watcherStorageUrl_/_watcherCheckLocation_" >> config-watcher.yml
fi
if ! `grep upgradeCheckIntervalSeconds config-watcher.yml > /dev/null`; then
  echo "upgradeCheckIntervalSeconds: 60" >> config-watcher.yml
fi
if ! `grep delegateCheckLocation config-watcher.yml > /dev/null`; then
  echo "delegateCheckLocation: _delegateStorageUrl_/_delegateCheckLocation_" >> config-watcher.yml
fi

if [ ! -e config-delegate.yml ]; then
  echo "accountId: _accountId_" > config-delegate.yml
  echo "accountSecret: _accountSecret_" >> config-delegate.yml
fi
test "$(tail -c 1 config-delegate.yml)" && `echo "" >> config-delegate.yml`
if ! `grep managerUrl config-delegate.yml > /dev/null`; then
  echo "managerUrl: _managerHostAndPort_/api/" >> config-delegate.yml
fi
if ! `grep verificationServiceUrl config-delegate.yml > /dev/null`; then
  echo "verificationServiceUrl: _managerHostAndPort_/verification/" >> config-delegate.yml
fi
if ! `grep watcherCheckLocation config-delegate.yml > /dev/null`; then
  echo "watcherCheckLocation: _watcherStorageUrl_/_watcherCheckLocation_" >> config-delegate.yml
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
if ! `grep proxy config-delegate.yml > /dev/null`; then
  echo "proxy: false" >> config-delegate.yml
fi
if ! `grep pollForTasks config-delegate.yml > /dev/null`; then
  if [ "$DEPLOY_MODE" == "ONPREM" ]; then
      echo "pollForTasks: true" >> config-delegate.yml
  else
      echo "pollForTasks: _pollForTasks_" >> config-delegate.yml
  fi
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [[ $1 == "upgrade" ]]; then
  echo "Upgrade"
  CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  mkdir -p watcherBackup.$CURRENT_VERSION
  cp watcher.jar watcherBackup.$CURRENT_VERSION
  $JRE_BINARY $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar watcher.jar config-watcher.yml upgrade $2
else
  if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`; then
    echo "Watcher already running"
  else
    nohup $JRE_BINARY $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar watcher.jar config-watcher.yml >nohup-watcher.out 2>&1 &
    sleep 1
    if [ -s nohup-watcher.out ]; then
      echo "Failed to start Watcher."
      echo "$(cat nohup-watcher.out)"
      exit 1
    else
      sleep 3
      if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`; then
        echo "Watcher started"
      else
        echo "Failed to start Watcher."
        echo "$(tail -n 30 watcher.log)"
        exit 1
      fi
    fi
  fi
fi
