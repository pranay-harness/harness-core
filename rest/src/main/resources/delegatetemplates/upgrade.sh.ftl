<#include "common.sh.ftl">

mkdir backup.$1
mv delegate.jar backup.$1

DELEGATE_URL="$(echo ${delegateMetadataUrl} | awk -F/ '{print $3}')/$(curl ${delegateMetadataUrl} | cut -d " " -f2)"
wget $DELEGATE_URL

if [ "$JRE_CHANGED" -eq "1"]
then
  rm jre
  ln -s $JRE_DIR jre
fi

$JRE_BINARY -jar delegate.jar config-delegate.yml

