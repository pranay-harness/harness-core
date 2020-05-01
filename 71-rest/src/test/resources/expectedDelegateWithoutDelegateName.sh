#!/bin/bash -ex

(
mkdir -p logs
echo
echo "` date +%d/%m/%Y%t%H:%M:%S `    ###########################"

if [ ! -e start.sh ]; then
  echo
  echo "Delegate must not be run from a different directory"
  echo
  exit 1
fi

JRE_DIR=jre1.8.0_191
JRE_BINARY=$JRE_DIR/bin/java
case "$OSTYPE" in
  solaris*)
    OS=solaris
    ;;
  darwin*)
    OS=macosx
    JRE_DIR=jre1.8.0_191.jre
    JRE_BINARY=$JRE_DIR/Contents/Home/bin/java
    ;;
  linux*)
    OS=linux
    ;;
  bsd*)
    echo "freebsd not supported."
    exit 1;
    ;;
  msys*)
    echo "For windows execute run.bat"
    exit 1;
    ;;
  cygwin*)
    echo "For windows execute run.bat"
    exit 1;
    ;;
  *)
    echo "unknown: $OSTYPE"
    ;;
esac

JVM_URL=http://localhost:8888/jre/8u191/jre-8u191-${OS}-x64.tar.gz

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

ULIM=$(ulimit -n)
echo "ulimit -n is set to $ULIM"
if [[ "$ULIM" == "unlimited" || $ULIM -lt 10000 ]]; then
  echo
  echo "WARNING: ulimit -n is too low ($ULIM). Minimum 10000 required."
  echo
fi

if [[ "$OSTYPE" == darwin* ]]; then
  MEM=$(top -l 1 -n 0 | grep PhysMem | cut -d ' ' -f 2 | cut -d 'G' -f 1)
  if [[ $MEM =~ "M" ]]; then
    MEM=$(($(echo $MEM | cut -d 'M' -f 1)/1024))
  fi
  echo "Memory is $MEM"
  if [[ $MEM -lt 6 ]]; then
    echo
    echo "WARNING: Not enough memory ($MEM). Minimum 6 GB required."
    echo
  fi
else
  MEM=$(free -m | grep Mem | awk '{ print $2 }')
  echo "Memory is $MEM MB"
  if [[ $MEM -lt 6000 ]]; then
    echo
    echo "WARNING: Not enough memory ($MEM MB). Minimum 6 GB required."
    echo
  fi
fi

export MANAGER_HOST_AND_PORT=https://localhost:9090
if [ -e proxy.config ]; then
  source proxy.config
  if [[ $PROXY_HOST != "" ]]; then
    echo "Using proxy $PROXY_SCHEME://$PROXY_HOST:$PROXY_PORT"
    if [[ $PROXY_USER != "" ]]; then
      export PROXY_USER
      if [[ "$PROXY_PASSWORD_ENC" != "" ]]; then
        export PROXY_PASSWORD=$(echo $PROXY_PASSWORD_ENC | openssl enc -d -a -des-ecb -K 4143434f554e)
      fi
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

export DEPLOY_MODE=KUBERNETES

if [[ $DEPLOY_MODE != "KUBERNETES" ]]; then
  echo "Checking Delegate latest version..."
  DELEGATE_STORAGE_URL=http://localhost:8888
  REMOTE_DELEGATE_LATEST=$(curl $MANAGER_PROXY_CURL -ks $DELEGATE_STORAGE_URL/delegateci.txt)
  REMOTE_DELEGATE_URL=$DELEGATE_STORAGE_URL/$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f2)
  REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_LATEST | cut -d " " -f1)

  if [ ! -e delegate.jar ]; then
    echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
    curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
    if [[ $REMOTE_DELEGATE_VERSION != $CURRENT_VERSION ]]; then
      echo "Downloading Delegate $REMOTE_DELEGATE_VERSION ..."
      mkdir -p backup.$CURRENT_VERSION
      cp delegate.jar backup.$CURRENT_VERSION
      curl $MANAGER_PROXY_CURL -#k $REMOTE_DELEGATE_URL -o delegate.jar
    fi
  fi
fi

if [ ! -e config-delegate.yml ]; then
  echo "accountId: ACCOUNT_ID" > config-delegate.yml
  echo "accountSecret: ACCOUNT_KEY" >> config-delegate.yml
fi
test "$(tail -c 1 config-delegate.yml)" && `echo "" >> config-delegate.yml`
if ! `grep managerUrl config-delegate.yml > /dev/null`; then
  echo "managerUrl: https://localhost:9090/api/" >> config-delegate.yml
fi
if ! `grep verificationServiceUrl config-delegate.yml > /dev/null`; then
  echo "verificationServiceUrl: https://localhost:9090/verification/" >> config-delegate.yml
fi
if ! `grep watcherCheckLocation config-delegate.yml > /dev/null`; then
  echo "watcherCheckLocation: http://localhost:8888/watcherci.txt" >> config-delegate.yml
else
  sed -i.bak "s|^watcherCheckLocation:.*$|watcherCheckLocation: http://localhost:8888/watcherci.txt|" config-delegate.yml
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

if ! `grep useCdn config-delegate.yml > /dev/null`; then
  echo "useCdn: false" >> config-delegate.yml
else
  sed -i.bak "s|^useCdn:.*$|useCdn: false|" config-delegate.yml
fi
if ! `grep cdnUrl config-delegate.yml > /dev/null`; then
  echo "cdnUrl: http://localhost:9500" >> config-delegate.yml
else
  sed -i.bak "s|^cdnUrl:.*$|cdnUrl: http://localhost:9500|" config-delegate.yml
fi

rm -f -- *.bak

export KUBECTL_VERSION=v1.12.2

export DELEGATE_PROFILE=QFWin33JRlKWKBzpzE5A9A

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"

if [[ $DEPLOY_MODE == "KUBERNETES" ]]; then
  echo "Starting delegate - version $2"
  $JRE_BINARY $PROXY_SYS_PROPS $OVERRIDE_TMP_PROPS -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -Dcom.sun.jndi.ldap.object.disableEndpointIdentification=true -jar $2/delegate.jar config-delegate.yml watched $1
else
  echo "Starting delegate - version $REMOTE_DELEGATE_VERSION"
  $JRE_BINARY $PROXY_SYS_PROPS $OVERRIDE_TMP_PROPS -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -Dcom.sun.jndi.ldap.object.disableEndpointIdentification=true -jar delegate.jar config-delegate.yml watched $1
fi

sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`; then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail -n 30 delegate.log)"
fi )
