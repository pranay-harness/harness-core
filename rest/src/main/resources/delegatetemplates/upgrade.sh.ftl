<#include "common.sh.ftl">

REMOTE_HOST=$(echo ${delegateMetadataUrl} | awk -F/ '{print $3}')
REMOTE_DELEGATE_METADATA=$(curl ${delegateMetadataUrl} --fail --silent --show-error)
REMOTE_DELEGATE_URL="$REMOTE_HOST/$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f2)"
REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f1)

KEEP_N_BACKUPS=3
CURRENT_VERSION=0

if [ -e delegate.jar ]
then
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")

  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) -eq 1 ]
  then
    mkdir -p backup.$CURRENT_VERSION
    cp delegate.jar backup.$CURRENT_VERSION
    curl -sk $REMOTE_DELEGATE_URL -o delegate.jar
    BACKUPS='backup.*/'
    qsort dircomp $BACKUPS
    for i in ${qsort_ret[@]:${KEEP_N_BACKUPS}}; do
      rm -rf ${i}
    done
  else
    exit 1
  fi
else
  curl -sk $REMOTE_DELEGATE_URL -o delegate.jar
fi

if [ ! -d  $JRE_DIR ]
then
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -skLO -H "Cookie: oraclelicense=accept-securebackup-cookie" $JVM_URL
  tar xzvf $JVM_TAR_FILENAME
  rm -rf jre
  ln -s $JRE_DIR jre
fi

export HOSTNAME
$JRE_BINARY -Ddelegatesourcedir=$DIR -jar delegate.jar config-delegate.yml upgrade
