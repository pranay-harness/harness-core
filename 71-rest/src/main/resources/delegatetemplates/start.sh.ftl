<#include "common.sh.ftl">

if [[ "$(ulimit -n)" == "unlimited" || $(ulimit -n) -lt 10000 ]]; then
  echo "ulimit -n is too low ($(ulimit -n)). Run the following command to set it to 10000 or greater:"
  echo
  echo "ulimit -n 10000"
  echo
  exit 1
fi

if [[ "$OSTYPE" == "darwin" ]]; then
  if [[ $(top -l 1 -n 0 | grep PhysMem | cut -d ' ' -f 2 | cut -d 'G' -f 1) -lt 6 ]]; then
    echo "Not enough memory ($(top -l 1 -n 0 | grep PhysMem | cut -d ' ' -f 2)). Minimum 6 GB required."
    exit 1
  fi
else
  if [[ $(free -m | grep Mem | awk '{ print $2 }') -lt 6000 ]]; then
    echo "Not enough memory ($(free -m | grep Mem | awk '{ print $2 }') MB). Minimum 6 GB required."
    exit 1
  fi
fi

if [ ! -e proxy.config ]; then
  echo "PROXY_HOST=" > proxy.config
  echo "PROXY_PORT=" >> proxy.config
  echo "PROXY_SCHEME=" >> proxy.config
  echo "PROXY_USER=" >> proxy.config
  echo "PROXY_PASSWORD=" >> proxy.config
fi
test "$(tail -c 1 proxy.config)" && `echo "" >> proxy.config`
if ! `grep NO_PROXY proxy.config > /dev/null`; then
  echo "NO_PROXY=" >> proxy.config
fi

source proxy.config
if [[ $PROXY_HOST != "" ]]
then
  echo "Using $PROXY_SCHEME proxy $PROXY_HOST:$PROXY_PORT"
  if [[ $PROXY_USER != "" ]]
  then
    export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_USER:$PROXY_PASSWORD@$PROXY_HOST:$PROXY_PORT
    PROXY_SYS_PROPS="-Dhttp.proxyUser=$PROXY_USER -Dhttp.proxyPassword=$PROXY_PASSWORD -Dhttps.proxyUser=$PROXY_USER -Dhttps.proxyPassword=$PROXY_PASSWORD "
  else
    export PROXY_CURL="-x "$PROXY_SCHEME"://"$PROXY_HOST:$PROXY_PORT
    export http_proxy=$PROXY_HOST:$PROXY_PORT
    export https_proxy=$PROXY_HOST:$PROXY_PORT
  fi
  PROXY_SYS_PROPS=$PROXY_SYS_PROPS" -DproxyScheme=$PROXY_SCHEME -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
fi

if [[ $NO_PROXY != "" ]]
then
  echo "No proxy for domain suffixes $NO_PROXY"
  export no_proxy=$NO_PROXY
  SYSTEM_PROPERTY_NO_PROXY=`echo $NO_PROXY | sed "s/\,/|*/g"`
  PROXY_SYS_PROPS=$PROXY_SYS_PROPS" -Dhttp.nonProxyHosts=*$SYSTEM_PROPERTY_NO_PROXY"
fi

echo $PROXY_SYS_PROPS

ACCOUNT_STATUS=$(curl $PROXY_CURL -#k ${managerHostAndPort}/api/account/${accountId}/status | cut -d ":" -f 3 | cut -d "," -f 1 | cut -d "\"" -f 2)
if [[ $ACCOUNT_STATUS == "DELETED" ]]
then
  rm README.txt delegate.sh proxy.config start.sh stop.sh
  touch __deleted__
  exit 0
fi

if [ ! -d $JRE_DIR  -o ! -d jre -o ! -e $JRE_BINARY ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl $PROXY_CURL -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  mkdir -p tmp
  mv $JVM_TAR_FILENAME tmp
  cd tmp
  tar xzf $JVM_TAR_FILENAME
  rm -rf ../$JRE_DIR
  mv $JRE_DIR_OLD ../$JRE_DIR
  cd ..
  rm -rf jre tmp
  ln -s $JRE_DIR jre
fi

if [ ! -d $JRE_DIR  -o ! -d jre -o ! -e $JRE_BINARY ]
then
  echo "No JRE available. Exiting."
  exit 1
fi

echo "Checking Watcher latest version..."
WATCHER_STORAGE_URL=${watcherStorageUrl}
REMOTE_WATCHER_LATEST=$(curl $PROXY_CURL -#k $WATCHER_STORAGE_URL/${watcherCheckLocation})
REMOTE_WATCHER_URL=$WATCHER_STORAGE_URL/$(echo $REMOTE_WATCHER_LATEST | cut -d " " -f2)
REMOTE_WATCHER_VERSION=$(echo $REMOTE_WATCHER_LATEST | cut -d " " -f1)

if [ ! -e watcher.jar ]
then
  echo "Downloading Watcher $REMOTE_WATCHER_VERSION ..."
  curl $PROXY_CURL -#k $REMOTE_WATCHER_URL -o watcher.jar
else
  WATCHER_CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [[ $REMOTE_WATCHER_VERSION != $WATCHER_CURRENT_VERSION ]]
  then
    echo "Downloading Watcher $REMOTE_WATCHER_VERSION ..."
    mkdir -p watcherBackup.$WATCHER_CURRENT_VERSION
    cp watcher.jar watcherBackup.$WATCHER_CURRENT_VERSION
    curl $PROXY_CURL -#k $REMOTE_WATCHER_URL -o watcher.jar
  fi
fi

export DEPLOY_MODE=${deployMode}

if [[ $DEPLOY_MODE != "KUBERNETES" ]]; then
  echo "Checking Delegate latest version..."
  DELEGATE_STORAGE_URL=${delegateStorageUrl}
  REMOTE_DELEGATE_LATEST=$(curl $PROXY_CURL -#k $DELEGATE_STORAGE_URL/${delegateCheckLocation})
  REMOTE_DELEGATE_URL=$DELEGATE_STORAGE_URL/$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f2)
  REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f1)

  if [ ! -e delegate.jar ]
  then
    echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
    curl $PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    DELEGATE_CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    if [[ $REMOTE_DELEGATE_VERSION != $DELEGATE_CURRENT_VERSION ]]
    then
      echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
      mkdir -p backup.$DELEGATE_CURRENT_VERSION
      cp delegate.jar backup.$DELEGATE_CURRENT_VERSION
      curl $PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
    fi
  fi
fi

if [ ! -e config-watcher.yml ]; then
  echo "accountId: ${accountId}" > config-watcher.yml
fi
test "$(tail -c 1 config-watcher.yml)" && `echo "" >> config-watcher.yml`
if ! `grep accountSecret config-watcher.yml > /dev/null`; then
  echo "accountSecret: ${accountSecret}" >> config-watcher.yml
fi
if ! `grep managerUrl config-watcher.yml > /dev/null`; then
  echo "managerUrl: ${managerHostAndPort}/api/" >> config-watcher.yml
fi
if ! `grep doUpgrade config-watcher.yml > /dev/null`; then
  echo "doUpgrade: true" >> config-watcher.yml
fi
if ! `grep upgradeCheckLocation config-watcher.yml > /dev/null`; then
  echo "upgradeCheckLocation: ${watcherStorageUrl}/${watcherCheckLocation}" >> config-watcher.yml
fi
if ! `grep upgradeCheckIntervalSeconds config-watcher.yml > /dev/null`; then
  echo "upgradeCheckIntervalSeconds: 60" >> config-watcher.yml
fi
if ! `grep delegateCheckLocation config-watcher.yml > /dev/null`
then
  echo "delegateCheckLocation: ${delegateStorageUrl}/${delegateCheckLocation}" >> config-watcher.yml
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [[ $1 == "upgrade" ]]
then
  echo "Upgrade"
  CURRENT_VERSION=$(unzip -c watcher.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  mkdir -p watcherBackup.$CURRENT_VERSION
  cp watcher.jar watcherBackup.$CURRENT_VERSION
  $JRE_BINARY $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar watcher.jar config-watcher.yml upgrade $2
else
  if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
  then
    echo "Watcher already running"
  else
    nohup $JRE_BINARY $PROXY_SYS_PROPS -Dwatchersourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar watcher.jar config-watcher.yml >nohup-watcher.out 2>&1 &
    sleep 1
    if [ -s nohup-watcher.out ]
    then
      echo "Failed to start Watcher."
      echo "$(cat nohup-watcher.out)"
      exit 1
    else
      sleep 3
      if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
      then
        echo "Watcher started"
      else
        echo "Failed to start Watcher."
        echo "$(tail -n 30 watcher.log)"
        exit 1
      fi
    fi
  fi
fi
