#!/usr/bin/env bash

echo "Checking whether docker containers are up and running..."

container_names=("mongoContainer" "harness-proxy", "harnessManager", "verificationService", "harness_ui", "learningEngine")

for container_name in "${container_names[@]}"
do
   if [ $(docker inspect -f '{{.State.Running}}' $container_name) = "true" ]; then
      echo "docker container $container_name is running successfully"
   else
      echo "docker container $container_name failed to start"
      exit -1
   fi
done





