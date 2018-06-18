#!/bin/bash

set -e

sudo service mongod stop
sleep 5
docker run -p 27017:27017 -d --rm mongo:3.6 || true
sleep 5

echo 'starting server'
export HOSTNAME
export SPLUNKML_ROOT=$(pwd)/python/splunk_intelligence
export SPLUNKML_ENVIRONMENT=REMOTE

mongo harness --eval "db.dropDatabase();"

#build python docker image in parrallel
set +e
echo "remove existing image"
docker rmi le_local || true
set -e
echo "build docker image in background"
nohup sh -c 'cd python/splunk_intelligence && make init && make dist && docker build --rm -t le_local .' > docker_container_build.log &
docker_container_build_pid=$!

if [[ -z "${SERVER_BUILD_DIR}" ]]; then
  echo "SERVER_BUILD_DIR not set, building server code"
  mvn -B clean install -DskipTests=true -DskipIntegrationTests=true
  java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
       -Xloggc:portal-gc-logs.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 \
       -Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar \
       -Dfile.encoding=UTF-8 -jar rest/target/rest-0.0.1-SNAPSHOT-capsule.jar rest/config.yml > portal.log 2>&1 &
else
  echo "SERVER_BUILD_DIR is set, using prebuilt server build"
  echo $SERVER_BUILD_DIR
  java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
         -Xloggc:portal-gc-logs.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 \
         -Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar \
         -Dfile.encoding=UTF-8 -jar $SERVER_BUILD_DIR/rest/target/rest-0.0.1-SNAPSHOT-capsule.jar rest/config.yml > portal.log 2>&1 &
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

# run data gen to load test data
mvn -B failsafe:integration-test -pl rest -Dit.test=DataGenUtil -P datagen -P integration-coverage
datagen_status=$?
if [[ $datagen_status -ne 0 ]] ; then
  echo 'Datagen failed';
  exit $datagen_status
fi
echo "datagen finished"
# specifying -DfailIfNoTests=false flag b/c we are using surefire on integration dir
mvn -B test -pl rest -Dtest=software.wings.integration.JenkinsIntegrationTest -DfailIfNoTests=false

jenkins_overwrite_status=$?
echo "JenkinsIntegrationTest finished with status $jenkins_overwrite_status"
if [[ jenkins_overwrite_status -ne 0 ]] ; then
  echo 'jenkins overwrite failed';
  exit $jenkins_overwrite_status
fi

#Delegate integration test. Don't run with UI integration tests
if [ "$TEST_SUITE" != "UI_INTEGRATION" ] ;
then
  mvn -B test -pl rest -Dtest=software.wings.integration.DelegateIntegrationTest -DfailIfNoTests=false
  delegateIntegrationTestResult=$?
  if [[ $delegateIntegrationTestResult -ne 0 ]] ;
  then
    echo 'Delegate integration test failed';
    exit $delegateIntegrationTestResult
  fi
fi

#run delegate
sed -i -e 's/^doUpgrade.*/doUpgrade: false/' delegate/config-delegate.yml
rm -rf $HOME/appagent/ver4.3.1.0/logs/
if [[ -z "${SERVER_BUILD_DIR}" ]]; then
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:delegate-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar delegate/target/delegate-0.0.1-SNAPSHOT-capsule.jar delegate/config-delegate.yml > delgate.out 2>&1 &
else
    java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:delegate-gc-logs.gc -XX:+UseParallelGC \
         -XX:MaxGCPauseMillis=500 -jar $SERVER_BUILD_DIR/delegate/target/delegate-0.0.1-SNAPSHOT-capsule.jar delegate/config-delegate.yml > delgate.out 2>&1 &
fi

# wait for delegate to start
echo 'wait for delegate to start'

mvn -B test -pl rest -Dtest=software.wings.integration.DelegateRegistrationIntegrationTest#shouldWaitForADelegateToRegister -DfailIfNoTests=false
foundRegisteredDelegate=$?
if [[ $foundRegisteredDelegate -ne 0 ]] ; then
  echo 'Delegate registration failed';
  exit $foundRegisteredDelegate
fi

#build and start learning engine
export HOSTNAME
echo "host is $HOSTNAME"
#wait for docker container to finish
echo "waiting for docker image to build"
wait $docker_container_build_pid
echo "finished waiting for docker image to build"
serviceSecret=`mongo harness --eval "db.serviceSecrets.find({ }, { serviceSecret: 1, _id: 0})"| grep serviceSecret | awk '{print $4}' | tr -d '"'`
echo $serviceSecret
server_url=https://$HOSTNAME:9090
echo $server_url
docker run -d -e server_url=$server_url -e service_secret=$serviceSecret -e https_port=10800  -e learning_env=integration-tests le_local

echo "listing containers after le_local was launched"
docker ps

