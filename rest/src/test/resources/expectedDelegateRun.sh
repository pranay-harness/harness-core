#!/bin/bash

vercomp () {
    if [[ $1 == $2 ]]
    then
        echo "0"
        return
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            echo "1"
            return
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            echo "2"
            return
        fi
    done
    echo "0"
}

JRE_DIR=jre1.8.0_131
JRE_BINARY=jre/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jre-8u131-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jre-8u131-macosx-x64.tar.gz
    JRE_DIR=jre1.8.0_131.jre
    JRE_BINARY=jre/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jre-8u131-linux-x64.tar.gz
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

if [ ! -d jre ]
then
  echo "Downloading JRE packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -#kLO -H "Cookie: oraclelicense=accept-securebackup-cookie" $JVM_URL
  echo "Extracting JRE packages..."
  tar xzf $JVM_TAR_FILENAME
  ln -s $JRE_DIR jre
fi

REMOTE_HOST=$(echo http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt | awk -F/ '{print $3}')
REMOTE_DELEGATE_METADATA=$(curl http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt --fail --silent --show-error)
REMOTE_DELEGATE_URL="$REMOTE_HOST/$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f2)"
REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f1)

if [ ! -e delegate.jar ]
then
  echo "Downloading Bot..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
else
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d "=" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) -eq 1 ]
  then
    echo "Downloading Bot..."
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  fi
fi

if [ ! -e config-delegate.yml ]
then
  echo "accountId: ACCOUNT_ID" > config-delegate.yml
  echo "accountSecret: ACCOUNT_KEY" >> config-delegate.yml
  echo "managerUrl: https://https://localhost:9090/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
  echo "localDiskPath: /tmp" >> config-delegate.yml
fi


if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Bot already running"
else
  export HOSTNAME
  export CAPSULE_CACHE_DIR="$DIR/.cache"
  rm -rf "$CAPSULE_CACHE_DIR"
  nohup $JRE_BINARY -Ddelegatesourcedir="$DIR" -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate.jar config-delegate.yml >nohup.out 2>&1 &
  echo "Bot started"
fi
