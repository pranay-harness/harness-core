#!/bin/bash -e

JRE_DIR_OLD=jre1.8.0_131
JRE_DIR=jre1.8.0_131_2
JRE_BINARY=jre/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=http://wingsdelegates.s3-website-us-east-1.amazonaws.com/jre/8u131/jre-8u131-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=http://wingsdelegates.s3-website-us-east-1.amazonaws.com/jre/8u131/jre-8u131-macosx-x64.tar.gz
    JRE_DIR=jre1.8.0_131.jre
    JRE_BINARY=jre/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=http://wingsdelegates.s3-website-us-east-1.amazonaws.com/jre/8u131/jre-8u131-linux-x64.tar.gz
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

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

REMOTE_DELEGATE_URL=http://localhost:8888/jobs/delegateci/9/delegate.jar
REMOTE_DELEGATE_VERSION=9.9.9

CURRENT_VERSION=0.0.0

if [ -e delegate.jar ]
then
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")

  if [[ $REMOTE_DELEGATE_VERSION != $CURRENT_VERSION ]]
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
  rm -rf $JRE_DIR_OLD
fi

export HOSTNAME
export CAPSULE_CACHE_DIR="$DIR/.cache"
echo "Delegate upgrading to version $REMOTE_DELEGATE_VERSION"
$JRE_BINARY -Ddelegatesourcedir="$DIR" -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml upgrade
sleep 3
if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Delegate started"
else
  echo "Failed to start Delegate."
  echo "$(tail delegate.log)"
fi
