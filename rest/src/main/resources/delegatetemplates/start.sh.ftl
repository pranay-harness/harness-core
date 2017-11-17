<#include "common.sh.ftl">

if [ ! -d $JRE_DIR ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  mkdir -p tmp
  mv $JVM_TAR_FILENAME tmp
  cd tmp
  tar xzf $JVM_TAR_FILENAME
  mv $JRE_DIR_OLD ../$JRE_DIR
  cd ..
  rm -rf jre tmp
  ln -s $JRE_DIR jre
else
  rm -rf run.sh upgrade.sh README.txt
  echo "Install the Harness Delegate by executing start.sh in this directory." >> README.txt
fi


REMOTE_WATCHER_URL=${watcherJarUrl}
REMOTE_WATCHER_VERSION=${watcherUpgradeVersion}

if [ ! -e watcher.jar ]
then
  echo "Downloading Watcher..."
  curl -#k $REMOTE_WATCHER_URL -o watcher.jar
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
  $JRE_BINARY -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml upgrade $2
else
  if [[ $1 == "transition" ]]
  then
    echo "Transition"
    CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    $JRE_BINARY -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar watcher.jar config-watcher.yml transition
  else
    if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
    then
      echo "Watcher already running"
    else
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
fi
