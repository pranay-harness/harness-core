#!/bin/bash
JRE_DIR=jre1.8.0_112
JRE_BINARY=jre/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u112-b15/jre-8u112-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u112-b16/jre-8u112-macosx-x64.tar.gz
    JRE_DIR=jre1.8.0_112.jre
    JRE_BINARY=jre/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=http://download.oracle.com/otn-pub/java/jdk/8u112-b16/jre-8u112-linux-x64.tar.gz
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

JRE_CHANGED=0

if [ ! -d  $JRE_DIR ]
then
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" $JVM_URL
  tar xzvf $JVM_TAR_FILENAME
  JRE_CHANGED=1
fi

if [ "$JRE_CHANGED" -eq "1" ]
then
  rm jre
fi

if [ ! -d  jre ]
then
  ln -s $JRE_DIR jre
fi

if [ ! -e delegate.jar ]
then
  DELEGATE_URL="$(echo https://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt | awk -F/ '{print $3}')/$(curl https://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt | cut -d " " -f2)"
  wget $DELEGATE_URL
fi

if [ ! -e config-delegate.yml ]
then
  echo "accountId: ACCOUNT_ID" > config-delegate.yml
  echo "accountSecret: ACCOUNT_KEY" >> config-delegate.yml
  echo "managerUrl: https://https://localhost:9090/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
fi

$JRE_BINARY -jar delegate.jar config-delegate.yml
