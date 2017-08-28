<#include "common.sh.ftl">

REMOTE_DELEGATE_URL=${delegateJarUrl}
REMOTE_DELEGATE_VERSION=${upgradeVersion}

CURRENT_VERSION=${currentVersion}

if [ -e delegate.jar ]
then
  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) != 0 ]
  then
    echo "Downloading Delegate..."
    mkdir -p backup.$CURRENT_VERSION
    cp delegate.jar backup.$CURRENT_VERSION
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    exit 1
  fi
else
  echo "Downloading Delegate..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
fi

if [ ! -d  $JRE_DIR ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -#kLO $JVM_URL
  echo "Extracting JRE packages..."
  tar xzf $JVM_TAR_FILENAME
  rm -rf jre
  ln -s $JRE_DIR jre
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"
rm -rf "$CAPSULE_CACHE_DIR"
echo "Delegate upgrading to version $REMOTE_DELEGATE_VERSION"
$JRE_BINARY -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml upgrade
sleep 3
if `pgrep -f "\-Ddelegatesourcedir"> /dev/null`
then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail delegate.log)"
fi
