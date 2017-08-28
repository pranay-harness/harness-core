<#include "common.sh.ftl">

if [ ! -d jre ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  tar xzf $JVM_TAR_FILENAME
  ln -s $JRE_DIR jre
fi


REMOTE_DELEGATE_URL=${delegateJarUrl}
REMOTE_DELEGATE_VERSION=${upgradeVersion}

CURRENT_VERSION=${currentVersion}

if [ ! -e delegate.jar ]
then
  echo "Downloading Delegate..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
else
  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) != 0 ]
  then
    echo "Downloading Delegate..."
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


if `pgrep -f "\-Ddelegatesourcedir"> /dev/null`
then
  echo "Delegate already running"
else
  export HOSTNAME
  export CAPSULE_CACHE_DIR="$DIR/.cache"
  rm -rf "$CAPSULE_CACHE_DIR"
  nohup $JRE_BINARY -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml >nohup.out 2>&1 &
  sleep 1
  if [ -s nohup.out ]
  then
    echo "Failed to start Delegate."
    echo "$(cat nohup.out)"
  else
    sleep 3
    if `pgrep -f "\-Ddelegatesourcedir"> /dev/null`
    then
      echo "Delegate started"
    else
      echo "Failed to start Delegate."
      echo "$(tail -n 50 delegate.log)"
    fi
  fi
fi
