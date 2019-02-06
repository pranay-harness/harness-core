#!/bin/bash

#stop mongo
echo "Stop mongod"
sudo service mongod stop

#kill vault
pgrep -f "vault"| xargs kill -9 || true

echo "kill delegate"
$JAVA_HOME/bin/jps -l | grep DelegateApplication | xargs kill -9 || true

echo "verification service"
$JAVA_HOME/bin/jps -l | grep VerificationServiceApplication | xargs kill -9 || true

echo "kill manager"
$JAVA_HOME/bin/jps -l | grep WingsApplication | xargs kill -9 || true

#kill le container
docker ps -a
# don't fail if the container is not presents
docker ps | grep le_local | awk '{print $1}'

# cleaning up all exited containers
docker ps -a | grep Exited | awk '{print $1}' | xargs docker rm || true

echo "about to call docker kill le_local"
docker ps | grep learning | awk '{print $1}' | xargs docker kill || true



