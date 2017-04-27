<#include "common.sh.ftl">

REMOTE_HOST=$(echo ${delegateMetadataUrl} | awk -F/ '{print $3}')
REMOTE_DELEGATE_METADATA=$(curl ${delegateMetadataUrl} --fail --silent --show-error)
REMOTE_DELEGATE_URL="$REMOTE_HOST/$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f2)"
REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f1)

CURRENT_VERSION=0

if [ -e delegate.jar ]
then
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")

  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) -eq 1 ]
  then
    echo "Downloading Bot..."
    mkdir -p backup.$CURRENT_VERSION
    cp delegate.jar backup.$CURRENT_VERSION
    curl -#k $REMOTE_DELEGATE_URL -o delegate.jar
  else
    exit 1
  fi
else
  echo "Downloading Bot..."
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
echo "Bot upgrading to version ${REMOTE_DELEGATE_VERSION}"
$JRE_BINARY -Ddelegatesourcedir=$DIR -jar delegate.jar config-delegate.yml upgrade
