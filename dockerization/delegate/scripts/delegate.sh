#!/bin/bash -e

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

if [ -z "$1" ]
then
  echo "This script is not meant to be executed directly. The watcher uses it to manage delegate processes."
  exit 0
fi

if [ -e proxy.config ]
then
  source proxy.config

  if [[ $PROXY_HOST != "" ]]
  then
    echo "Using $PROXY_SCHEME proxy $PROXY_HOST:$PROXY_PORT"
    export http_proxy=$PROXY_HOST:$PROXY_PORT
    export https_proxy=$PROXY_HOST:$PROXY_PORT
    PROXY_SYS_PROPS="-DproxyScheme=$PROXY_SCHEME -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
  fi
  if [[ $NO_PROXY != "" ]]
  then
    echo "No proxy for domain suffixes $NO_PROXY"
    export no_proxy=$NO_PROXY
    SYSTEM_PROPERTY_NO_PROXY=`echo $NO_PROXY | sed "s/\,/|*/g"`
    PROXY_SYS_PROPS=$PROXY_SYS_PROPS" -Dhttp.nonProxyHosts=*$SYSTEM_PROPERTY_NO_PROXY"
    echo $PROXY_SYS_PROPS
  fi
fi

REMOTE_DELEGATE_URL=${delegateJarUrl}
REMOTE_DELEGATE_VERSION=${upgradeVersion}
DEPLOY_MODE=${deployMode}

if [ ! -e delegate.jar ]
then
  echo "Downloading Delegate..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
else
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_DELEGATE_VERSION != $CURRENT_VERSION ]]
  then
    echo "Downloading Delegate..."
    mkdir -p backup.$CURRENT_VERSION
    cp delegate.jar backup.$CURRENT_VERSION
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  fi
fi

if [ ! -e config-delegate.yml ]
then
  echo "accountId: ${accountId}" > config-delegate.yml
  echo "accountSecret: ${accountSecret}" >> config-delegate.yml
  echo "managerUrl: ${managerHostAndPort}/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
  echo "doUpgrade: true" >> config-delegate.yml
  echo "description: description here" >> config-delegate.yml
  echo "localDiskPath: /tmp" >> config-delegate.yml
  echo "maxCachedArtifacts: 2" >> config-delegate.yml
  echo "proxy: false" >> config-delegate.yml
  if [ "$DEPLOY_MODE" == "ONPREM" ]; then
      echo "pollForTasks: true" >> config-delegate.yml
  else
      echo "pollForTasks: false" >> config-delegate.yml
  fi
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"
echo "Starting delegate - version $REMOTE_DELEGATE_VERSION"
java $PROXY_SYS_PROPS -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml watched $1
sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail -n 30 delegate.log)"
fi
