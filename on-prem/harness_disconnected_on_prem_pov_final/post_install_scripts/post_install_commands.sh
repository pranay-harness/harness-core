#!/usr/bin/env bash
# Copyright 2019 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

echo "Checking whether docker containers are up and running..."

container_names=("mongoContainer" "harness-proxy" "harnessManager" "verificationService" "harness_ui" "learningEngine")

for container_name in "${container_names[@]}"
do
   if [[ $(docker inspect -f '{{.State.Running}}' ${container_name}) = "true" ]]; then
      echo "✓ docker container $container_name is running successfully"
   else
      echo "✗ docker container $container_name failed to start"
      exit -1
   fi
done

INFRA_PROPERTY_FILE=../inframapping.properties

source ../utils.sh

function checkUIisUp() {
   host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")

   url="${host1}:7143"

   curl -sI -m 5 ${url} | grep -q "200 OK"
   if [[ $? -eq 0 ]]; then
       echo "✓ UI is up"
   else
       echo "✗ UI is NOT up"
   fi
}

function checkManagerIsHealthy() {
   host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")

   url="${host1}:7143/api/health"
   curl -s -m 5 ${url} | grep -q "healthy"

   if [[ $? -eq 0 ]]; then
       echo "✓ Manager is healthy"
   else
       echo "✗ Manager is not healthy"
   fi
}

function checkVerificationServiceIsHealthy() {
   host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")

   url="${host1}:7143/verification/health"
   curl -s -m 5 ${url} | grep -q "healthy"

   if [[ $? -eq 0 ]]; then
       echo "✓ Verification Service is healthy"
   else
       echo "✗ Verification Service is not healthy"
   fi
}

function checkDelegateAndApiVersionMatch() {
   host1=$(getProperty "$INFRA_PROPERTY_FILE" "HOST1_IP_ADDRESS")

   url="${host1}:7143/storage/wingsdelegates/delegateprod.txt"
   curl -sI -m 5 ${url} | grep -q "200 OK"

   if [[ $? -eq 0 ]]; then
       echo "✓ Delegate version API is up"
   else
       echo "✗ Delegate version API failed."
   fi
   delegateVersion=$(curl -s -m 5 ${url} | awk '{print $1}')
   apiVersionEndpoint="${host1}:7143/api/version"
   curl -s -m 5 ${apiVersionEndpoint} | grep -q "${delegateVersion}"
   if [[ $? -eq 0 ]]; then
       echo "✓ Delegate version and API version match."
   else
       echo "✗ Delegate version did not match API version."
   fi
}

echo "Checking service API reachability."
checkUIisUp
checkManagerIsHealthy
checkVerificationServiceIsHealthy
checkDelegateAndApiVersionMatch
