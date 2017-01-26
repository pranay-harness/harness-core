<#include "common.sh.ftl">

if [ ! -d jre ]
then
  JVM_TAR_FILENAME=$(basename "$JVM_URL")
  curl -skLO -H "Cookie: oraclelicense=accept-securebackup-cookie" $JVM_URL
  tar xzvf $JVM_TAR_FILENAME
  ln -s $JRE_DIR jre
fi

REMOTE_HOST=$(echo ${delegateMetadataUrl} | awk -F/ '{print $3}')
REMOTE_DELEGATE_METADATA=$(curl ${delegateMetadataUrl} --fail --silent --show-error)
REMOTE_DELEGATE_URL="$REMOTE_HOST/$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f2)"
REMOTE_DELEGATE_VERSION=$(echo $REMOTE_DELEGATE_METADATA | cut -d " " -f1)

if [ ! -e delegate.jar ]
then
  curl -sk $REMOTE_DELEGATE_URL -o delegate.jar
else
  CURRENT_VERSION=$(unzip -c delegate.jar META-INF/MANIFEST.MF | grep Application-Version | cut -d ":" -f2 | tr -d " " | tr -d "\r" | tr -d "\n")
  if [ $(vercomp $REMOTE_DELEGATE_VERSION $CURRENT_VERSION) -eq 1 ]
  then
    curl -sk $REMOTE_DELEGATE_URL -o delegate.jar
  fi
fi

if [ ! -e config-delegate.yml ]
then
  echo "accountId: ${accountId}" > config-delegate.yml
  echo "accountSecret: ${accountSecret}" >> config-delegate.yml
  echo "managerUrl: https://${managerHostAndPort}/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
  echo "localDiskPath: /tmp" >> config-delegate.yml
fi


if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  echo "Delegate already running."
else
  nohup $JRE_BINARY -Ddelegatesourcedir=$DIR -jar delegate.jar config-delegate.yml >nohup.out 2>&1 &
  echo "Delegate started."
fi
