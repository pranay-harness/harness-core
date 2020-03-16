#!/bin/bash -e

if [ ! -e start.sh ]; then
  echo
  echo "Delegate must not be run from a different directory"
  echo
  exit 1
fi

JRE_DIR=${jre_dir}
JRE_BINARY=$JRE_DIR/bin/java
case "$OSTYPE" in
  solaris*)
    JVM_URL=${delegateStorageUrl}/${jre_tar_path_solaris}
    ;;
  darwin*)
    JVM_URL=${delegateStorageUrl}/${jre_tar_path_macos}
    JRE_DIR=${jre_dir_macos}
    JRE_BINARY=$JRE_DIR/Contents/Home/bin/java
    ;;
  linux*)
    JVM_URL=${delegateStorageUrl}/${jre_tar_path_linux}
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
