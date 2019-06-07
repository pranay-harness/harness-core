#!/bin/bash -e

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
    JVM_URL=${delegateStorageUrl}/jre/8u191/jre-8u191-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=${delegateStorageUrl}/jre/8u191/jre-8u191-macosx-x64.tar.gz
    JRE_DIR=jre1.8.0_191.jre
    JRE_BINARY=$JRE_DIR/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=${delegateStorageUrl}/jre/8u191/jre-8u191-linux-x64.tar.gz
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

<#noparse>
SOURCE="${BASH_SOURCE[0]}"
</#noparse>
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
