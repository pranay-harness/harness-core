<#include "common.sh.ftl">

if [ -z "$1" ]
then
  echo "This script is not meant to be executed directly. The watcher uses it to manage delegate processes."
  exit 0
fi

REMOTE_DELEGATE_URL=${delegateJarUrl}
REMOTE_DELEGATE_VERSION=${upgradeVersion}

CURRENT_VERSION=${currentVersion}

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
  echo "managerUrl: https://${managerHostAndPort}/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
  echo "doUpgrade: true" >> config-delegate.yml
  echo "localDiskPath: /tmp" >> config-delegate.yml
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"
echo "Starting delegate - version $REMOTE_DELEGATE_VERSION"
$JRE_BINARY -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml watched $1
sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail -n 30 delegate.log)"
fi
