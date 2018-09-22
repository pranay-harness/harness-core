#!/bin/bash -e

if [ ! -e start.sh ]; then
  echo
  echo "Delegate must not be run from a different directory"
  echo
  exit 1
fi

JRE_DIR_OLD=jre1.8.0_131
JRE_DIR=jre1.8.0_131_2
JRE_BINARY=jre/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=http://localhost:8888/jre/8u131/jre-8u131-solaris-x64.tar.gz
    ;;
  darwin*)
    JVM_URL=http://localhost:8888/jre/8u131/jre-8u131-macosx-x64.tar.gz
    JRE_DIR_OLD=jre1.8.0_131.jre
    JRE_BINARY=jre/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=http://localhost:8888/jre/8u131/jre-8u131-linux-x64.tar.gz
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

if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
then
  i=0
  stopped=0
  while [ "$i" -le 30 ]
  do
    if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
    then
      pgrep -f "\-Dwatchersourcedir=$DIR" | xargs kill
      if [ "$i" -gt 0 ]
      then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Watcher stopped"
      stopped=1
      break
    fi
  done
  if [ "$stopped" -eq 0 ]
  then
    echo "Unable to stop watcher in 30 seconds."
    exit 1
  fi
else
  echo "Watcher not running"
fi

if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  i=0
  while [ "$i" -le 30 ]
  do
    if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
    then
      pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs kill
      if [ "$i" -gt 0 ]
      then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Delegate stopped"
      rm -rf msg
      exit 0
    fi
  done
  echo "Unable to stop delegate in 30 seconds."
  exit 1
else
  echo "Delegate not running"
  rm -rf msg
  exit 0
fi
