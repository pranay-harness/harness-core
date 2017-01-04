<#include "common.sh.ftl">

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
  DELEGATE_URL="$(echo ${delegateMetadataUrl} | awk -F/ '{print $3}')/$(curl ${delegateMetadataUrl} | cut -d " " -f2)"
  wget $DELEGATE_URL
fi

if [ ! -e config-delegate.yml ]
then
  echo "accountId: ${accountId}" > config-delegate.yml
  echo "accountSecret: ${accountSecret}" >> config-delegate.yml
  echo "managerUrl: https://${managerHostAndPort}/api/" >> config-delegate.yml
  echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
fi

$JRE_BINARY -jar delegate.jar config-delegate.yml
