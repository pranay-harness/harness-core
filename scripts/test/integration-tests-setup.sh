#!/bin/bash

set -ex

docker stop $(docker ps -aq) || true
docker rm $(docker ps -aq) || true

sudo service mongod restart
sleep 5

#kill vault
pgrep -f "vault"| xargs kill -9 || true

echo 'starting vault server'
touch vault.log
~/vault server --dev -dev-root-token-id="root" -dev-listen-address="127.0.0.1:8200" > vault.log &
~/vault server --dev -dev-root-token-id="root" -dev-listen-address="127.0.0.1:8300" > vault2.log &

i=0
while [ "$i" -lt 30 ]
do
  vault_token=`cat vault.log | grep "Root Token:" | awk '{print $3}'`
  if [ -z "$vault_token" ]
  then
    echo 'sleeping since did not find vault token yet'
    sleep 2
    i=$((i+1))
  else
    echo "found vault token"
    echo $vault_token
    break
  fi
done

if [ -z "$vault_token" ]
  then
    echo 'could not find vault token. exiting....'
    exit 1
fi

echo 'starting server'
export HOSTNAME
export SPLUNKML_ROOT=$(pwd)/python/splunk_intelligence
export SPLUNKML_ENVIRONMENT=REMOTE

mongo harness --eval "db.dropDatabase();"

echo "build docker image in background"
#nohup sh -c 'cd python/splunk_intelligence && make init && make dist && docker build --rm -t le_local .' > docker_container_build.log &
#docker_container_build_pid=$!

if [[ -z "${SERVER_BUILD_DIR}" ]]; then
  echo "SERVER_BUILD_DIR not set, building server code"
  mvn ${MAVEN_ARGS} -T 1C clean install -DskipTests=true -DskipIntegrationTests=true
  java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
       -Xloggc:portal-gc-logs.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 \
       -Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar \
       -Dfile.encoding=UTF-8 -jar 400-rest/target/rest-capsule.jar 400-rest/config.yml > portal.log 2>&1 &
else
  echo "SERVER_BUILD_DIR is set, using prebuilt server build"
  echo $SERVER_BUILD_DIR
  java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
         -Xloggc:portal-gc-logs.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 \
         -Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar \
         -Dfile.encoding=UTF-8 -jar $SERVER_BUILD_DIR/400-rest/target/rest-capsule.jar 400-rest/config.yml > portal.log 2>&1 &
fi

echo 'sleep for server to start'
set +e
output=$(curl -sSk https://localhost:9090/api/version)
status=$?
count=1
while [[ $status == 7 && $count -lt 60 ]]
do
  sleep 5
  output=$(curl -sSk https://localhost:9090/api/version)
  status=$?
  count=`expr $count + 1`
done

if [ $count -eq 60 ]
then
  echo 'server failed to start'
  exit 1
fi

echo "server started, going to run DataGen util"
set -e

echo "Running data gen"
# run data gen to load test data
#run datagen
if [[ -z "${SERVER_BUILD_DIR}" ]]; then
    java -Xmx1024m -jar 160-model-gen-tool/target/model-gen-tool-capsule.jar 160-model-gen-tool/config-datagen.yml
else
    java -Xmx1024m -jar $SERVER_BUILD_DIR/160-model-gen-tool/target/model-gen-tool-capsule.jar 160-model-gen-tool/config-datagen.yml
fi

echo "datagen finished"

# specifying -DfailIfNoTests=false flag b/c we are using surefire on integration dir
mvn ${MAVEN_ARGS} test -pl 400-rest -Dtest=software.wings.integration.JenkinsIntegrationTest -DfailIfNoTests=false

jenkins_overwrite_status=$?
echo "JenkinsIntegrationTest finished with status $jenkins_overwrite_status"
if [[ jenkins_overwrite_status -ne 0 ]] ; then
  echo 'jenkins overwrite failed';
  exit $jenkins_overwrite_status
fi

#run verification engine
sed -i -e 's/^doUpgrade.*/doUpgrade: false/' 270-verification/verification-config.yml
if [[ -z "${SERVER_BUILD_DIR}" ]]; then
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:verification-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar 270-verification/target/verification-capsule.jar 270-verification/verification-config.yml > verification.log 2>&1 &
else
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:verification-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar $SERVER_BUILD_DIR/verification/target/verification-capsule.jar 270-verification/verification-config.yml > verification.log 2>&1 &
fi

# wait for verification to start
echo 'wait for verification engine to start'

#run delegate
sed -i -e 's/^doUpgrade.*/doUpgrade: false/' 260-delegate/config-delegate.yml
rm -rf $HOME/appagent/ver4.3.1.0/logs/
if [[ -z "${SERVER_BUILD_DIR}" ]]; then
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:delegate-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar 260-delegate/target/delegate-capsule.jar 260-delegate/config-delegate.yml > delgate.out 2>&1 &
else
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:delegate-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar $SERVER_BUILD_DIR/260-delegate/target/delegate-capsule.jar 260-delegate/config-delegate.yml > delgate.out 2>&1 &
fi

# wait for delegate to start
echo 'wait for delegate to start'

mvn ${MAVEN_ARGS} test -pl 400-rest -Dtest=software.wings.integration.DelegateRegistrationIntegrationTest#shouldWaitForADelegateToRegister -DfailIfNoTests=false
foundRegisteredDelegate=$?
if [[ $foundRegisteredDelegate -ne 0 ]] ; then
  echo 'Delegate registration failed';
  exit $foundRegisteredDelegate
fi

export HOSTNAME
echo "host is $HOSTNAME"

serviceSecret=`mongo harness --eval "db.serviceSecrets.find({ \"serviceType\" : \"LEARNING_ENGINE\" }, { serviceSecret: 1, _id: 0})"| grep serviceSecret | awk '{print $4}' | tr -d '"'`
echo $serviceSecret
server_url=https://$HOSTNAME:7070
echo $server_url
mkdir -p le_logs
rm -rf le_logs/le.log
docker pull harness/learning-engine-prod:latest && docker run -d -e server_url=$server_url -e service_secret=$serviceSecret -e https_port=10800  -e learning_env=on_prem -v $(pwd)/le_logs/:/home/harness/logs harness/learning-engine-prod:latest

echo "listing containers after le_local was launched"
docker ps

# Vault integration test need to be run first to avoid interfering with other integration test such as
# Sumo/Elk integration tests etc.
mvn ${MAVEN_ARGS} test -pl 400-rest -Dtest=software.wings.integration.security.VaultIntegrationTest -DfailIfNoTests=false
mvn ${MAVEN_ARGS} test -pl 400-rest -Dtest=software.wings.integration.security.KmsIntegrationTest -DfailIfNoTests=false


