#!/bin/bash -e

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

if [ ! -e proxy.config ]
then
  echo "PROXY_HOST=" > proxy.config
  echo "PROXY_PORT=" >> proxy.config
  echo "PROXY_SCHEME=" >> proxy.config
  echo "NO_PROXY=" >> proxy.config
fi

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

REMOTE_WATCHER_URL=${watcherJarUrl}
REMOTE_WATCHER_VERSION=${watcherUpgradeVersion}

if [ ! -e watcher.jar ]
then
  echo "Downloading Watcher..."
  curl -#k $REMOTE_WATCHER_URL -o watcher.jar
else
  WATCHER_CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_WATCHER_VERSION != $WATCHER_CURRENT_VERSION ]]
  then
    echo "Downloading Watcher..."
    mkdir -p watcherBackup.$WATCHER_CURRENT_VERSION
    cp watcher.jar watcherBackup.$WATCHER_CURRENT_VERSION
    curl -#k $REMOTE_WATCHER_URL -o watcher.jar
  fi
fi

REMOTE_DELEGATE_URL=${delegateJarUrl}
REMOTE_DELEGATE_VERSION=${upgradeVersion}

if [ ! -e delegate.jar ]
then
  echo "Downloading Delegate..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
else
  DELEGATE_CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_DELEGATE_VERSION != $DELEGATE_CURRENT_VERSION ]]
  then
    echo "Downloading Delegate..."
    mkdir -p backup.$DELEGATE_CURRENT_VERSION
    cp delegate.jar backup.$DELEGATE_CURRENT_VERSION
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  fi
fi

if [ ! -e config-watcher.yml ]
then
  echo "accountId: ${accountId}" > config-watcher.yml
  echo "doUpgrade: true" >> config-watcher.yml
  echo "upgradeCheckLocation: ${watcherCheckLocation}" >> config-watcher.yml
  echo "upgradeCheckIntervalSeconds: 60" >> config-watcher.yml
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [[ $1 == "upgrade" ]]
then
  echo "Upgrade"
  CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  mkdir -p watcherBackup.$CURRENT_VERSION
  cp watcher.jar watcherBackup.$CURRENT_VERSION
  java $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml upgrade $2
else
  if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
  then
    echo "Watcher already running"
  else
    nohup java $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml >nohup-watcher.out 2>&1 &
    sleep 1
    if [ -s nohup-watcher.out ]
    then
      echo "Failed to start Watcher."
      echo "$(cat nohup-watcher.out)"
    else
      sleep 3
      if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
      then
        echo "Watcher started"
      else
        echo "Failed to start Watcher."
        echo "$(tail -n 30 watcher.log)"
      fi
    fi
  fi
fi
