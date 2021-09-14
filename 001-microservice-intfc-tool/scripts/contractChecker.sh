#!/usr/bin/env bash
set -ex

#Find the current running build version
BUILDNO=curl https://app.harness.io/prod1/pipeline/api/version | jq '.resource.versionInfo.buildNo'
PATCH=curl https://app.harness.io/prod1/pipeline/api/version | jq '.resource.versionInfo.patch'

#Find the build version of to be deployed service
CURRENTBUILDPATCHDETAILS=${artifact.buildNo}

#Fetch protocol-info files
gsutil cp gs:feature-delegate-jars/artifacts/openjdk-8u242/protocol-data/pipeline-service/pipeline-service-protocol-$CURRENTBUILDPATCHDETAILS.info .
gsutil cp gs:feature-delegate-jars/artifacts/openjdk-8u242/protocol-data/pipeline-service/pipeline-service-protocol-$BUILDNO-$PATCH.info .

difference=`diff pipeline-service-protocol-700119-000.info pipeline-service-protocol-700120-000.info`

if ["$difference" == ""];then
 echo "Same"
else
 echo "Different"
fi