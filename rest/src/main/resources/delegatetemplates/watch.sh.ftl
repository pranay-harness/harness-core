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


REMOTE_WATCHER_URL=${watcherJarUrl}
REMOTE_WATCHER_VERSION=${watcherUpgradeVersion}

if [ ! -e watcher.jar ]
then
  echo "Downloading Watcher..."
  curl -#k $REMOTE_WATCHER_URL -o watcher.jar
else
  CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_WATCHER_VERSION != $CURRENT_VERSION ]]
  then
    echo "Downloading Watcher..."
    curl -#k $REMOTE_WATCHER_URL -o watcher.jar
  fi
fi

if [ ! -e config-watcher.yml ]
then
  echo "accountId: ${accountId}" > config-watcher.yml
  echo "doUpgrade: true" >> config-watcher.yml
  echo "upgradeCheckLocation: ${watcherCheckLocation}" >> config-watcher.yml
  echo "upgradeCheckIntervalSeconds: 300" >> config-watcher.yml
fi


if [[ $1 == "upgrade" ]]
then
  echo "Upgrade"
  CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  mkdir -p watcherBackup.$CURRENT_VERSION
  cp watcher.jar watcherBackup.$CURRENT_VERSION
  export HOSTNAME
  export CAPSULE_CACHE_DIR="$DIR/.cache"
  $JRE_BINARY -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml upgrade
else
  if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
  then
    echo "Watcher already running"
  else
    export HOSTNAME
    export CAPSULE_CACHE_DIR="$DIR/.cache"
    nohup $JRE_BINARY -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml >nohup-watcher.out 2>&1 &
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
