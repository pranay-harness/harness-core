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

JRE_DIR=jre1.8.0_121
JRE_BINARY=jre/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jre-8u121-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jre-8u121-macosx-x64.tar.gz
    JRE_DIR=jre1.8.0_121.jre
    JRE_BINARY=jre/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jre-8u121-linux-x64.tar.gz
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

REMOTE_HOST=$(echo http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt | awk -F/ '{print $3}')
REMOTE_DELEGATE_METADATA=$(curl http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt --fail --silent --show-error)
REMOTE_DELEGATE_URL="$REMOTE_HOST/$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f2)"
REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f1)

CURRENT_VERSION=0

if [ -e delegate.jar ]
then
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")

  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) -eq 1 ]
  then
    echo "Downloading Wings Bot..."
    mkdir -p backup.$CURRENT_VERSION
    cp delegate.jar backup.$CURRENT_VERSION
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    exit 1
  fi
else
  echo "Downloading Wings Bot..."
  curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
fi

if [ ! -d  $JRE_DIR ]
then
  echo "Downloading packages..."
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -#kLO -H "Cookie: oraclelicense=accept-securebackup-cookie" $JVM_URL
  echo "Extracting packages..."
  tar xzf $JVM_TAR_FILENAME
  rm -rf jre
  ln -s $JRE_DIR jre
fi

export HOSTNAME
echo "Wings Bot upgrading..."
$JRE_BINARY -Ddelegatesourcedir=$DIR -jar delegate.jar config-delegate.yml upgrade
echo "Wings Bot upgraded"
