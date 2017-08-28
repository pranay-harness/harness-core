#!/bin/bash -e

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
      exit 0
    fi
  done
  echo "Unable to stop delegate in 30 seconds."
  exit 1
else
  echo "Delegate not running"
  exit 0
fi
