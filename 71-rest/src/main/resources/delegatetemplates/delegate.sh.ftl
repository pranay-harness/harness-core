<#include "common.sh.ftl">

if [ -z "$1" ]
then
  echo "This script is not meant to be executed directly. The watcher uses it to manage delegate processes."
  exit 0
fi

if [[ "$(ulimit -n)" == "unlimited" || $(ulimit -n) -lt 10000 ]]; then
  echo
  echo "WARNING: ulimit -n is too low ($(ulimit -n)). Minimum 10000 required."
  echo
fi

if [[ "$OSTYPE" == darwin* ]]; then
  if [[ $(top -l 1 -n 0 | grep PhysMem | cut -d ' ' -f 2 | cut -d 'G' -f 1) -lt 6 ]]; then
    echo
    echo "WARNING: Not enough memory ($(top -l 1 -n 0 | grep PhysMem | cut -d ' ' -f 2)). Minimum 6 GB required."
    echo
  fi
else
  if [[ $(free -m | grep Mem | awk '{ print $2 }') -lt 6000 ]]; then
    echo
    echo "WARNING: Not enough memory ($(free -m | grep Mem | awk '{ print $2 }') MB). Minimum 6 GB required."
    echo
  fi
fi

if [ -e proxy.config ]
then
  source proxy.config

  # when using authenticated proxy, need to use format curl -x <http/s://username:password@host:port> <actual_url>
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
fi

if [ ! -d $JRE_DIR -o ! -e $JRE_BINARY ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl $PROXY_CURL -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  rm -rf $JRE_DIR
  tar xzf $JVM_TAR_FILENAME
  rm -f $JVM_TAR_FILENAME
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
    CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    if [[ $REMOTE_DELEGATE_VERSION != $CURRENT_VERSION ]]
    then
      echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
      mkdir -p backup.$CURRENT_VERSION
      cp delegate.jar backup.$CURRENT_VERSION
      curl $PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
    fi
  fi
fi

if [ ! -e config-delegate.yml ]; then
  echo "accountId: ${accountId}" > config-delegate.yml
  echo "accountSecret: ${accountSecret}" >> config-delegate.yml
fi
test "$(tail -c 1 config-delegate.yml)" && `echo "" >> config-delegate.yml`
if ! `grep managerUrl config-delegate.yml > /dev/null`; then
  echo "managerUrl: ${managerHostAndPort}/api/" >> config-delegate.yml
fi
if ! `grep verificationServiceUrl config-delegate.yml > /dev/null`; then
  echo "verificationServiceUrl: ${managerHostAndPort}/verification/" >> config-delegate.yml
fi
if ! `grep watcherCheckLocation config-delegate.yml > /dev/null`
then
  echo "watcherCheckLocation: ${watcherStorageUrl}/${watcherCheckLocation}" >> config-delegate.yml
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
      echo "pollForTasks: false" >> config-delegate.yml
  fi
fi

export KUBECTL_VERSION=${kubectlVersion}

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [[ $DEPLOY_MODE == "KUBERNETES" ]]; then
  echo "Starting delegate - version $2"
  $JRE_BINARY $PROXY_SYS_PROPS -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar $2/delegate.jar config-delegate.yml watched $1
else
  echo "Starting delegate - version $REMOTE_DELEGATE_VERSION"
  $JRE_BINARY $PROXY_SYS_PROPS -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar delegate.jar config-delegate.yml watched $1
fi

sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail -n 30 delegate.log)"
fi
