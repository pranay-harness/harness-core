#!/bin/bash
sudo service mongod restart
mvn clean install -DskipTests=true
mvn test-compile
cd python/splunk_intelligence; make init; make dist;
cd ../../

echo 'starting server'
export HOSTNAME
export SPLUNKML_ROOT=$(pwd)/python/splunk_intelligence
export SPLUNKML_ENVIRONMENT=REMOTE
java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
     -Xloggc:portal-gc-logs.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar \
     -javaagent:$HOME/appagent/javaagent.jar -Dappdynamics.agent.nodeName=manager \
     -Dfile.encoding=UTF-8 -jar rest/target/rest-0.0.1-SNAPSHOT-capsule.jar rest/config.yml > portal.log 2>&1 &

echo 'sleep for server to start'
#wait for server to start
output=$(curl -sS https://localhost:9090/api/version)
status=$?
count=1
while [[ $status == 7 && $count -lt 20 ]]
do
sleep 5
output=$(curl -sS https://localhost:9090/api/version)
status=$?
count=`expr $count + 1`
done

if [ $count -eq 20 ]
then
  echo 'server failed to start'
  exit 1
fi


#run data gen to load test data
mvn test -pl rest -Dtest=software.wings.integration.DataGenUtil
datagen_status=$?
if [[ $datagen_status -ne 0 ]] ; then
  echo 'Datagen failed';
  exit $datagen_status
fi


mvn test -pl rest -Dtest=software.wings.integration.JenkinsIntegrationTest

jenkins_overwrite_status=$?
if [[ jenkins_overwrite_status -ne 0 ]] ; then
  echo 'jenkins overwrite failed';
  exit $jenkins_overwrite_status
fi

#Delegate integration test. Don't run with UI integration tests
if [ "$TEST_SUITE" != "UI_INTEGRATION" ] ;
then
  mvn test -pl rest -Dtest=software.wings.integration.DelegateIntegrationTest
  delegateIntegrationTestResult=$?
  if [[ $delegateIntegrationTestResult -ne 0 ]] ;
  then
    echo 'Delegate integration test failed';
    exit $delegateIntegrationTestResult
  fi
fi

#run delegate
sed -i -e 's/^doUpgrade.*/doUpgrade: false/' config-delegate.yml
rm -rf $HOME/appagent/ver4.3.1.0/logs/
java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:delegate-gc-logs.gc -XX:+UseParallelGC \
     -javaagent:$HOME/appagent/javaagent.jar -Dappdynamics.agent.nodeName=delegate \
     -XX:MaxGCPauseMillis=500 -jar delegate/target/delegate-0.0.1-SNAPSHOT-capsule.jar delegate/config-delegate.yml > delgate.out 2>&1 &

#wait for delegate to start
echo 'wait for delegate to start'

#wait for delegate to start
mvn test -pl rest -Dtest=software.wings.integration.DelegateRegistrationIntegrationTest#shouldWaitForADelegateToRegister
foundRegisteredDelegate=$?
if [[ $foundRegisteredDelegate -ne 0 ]] ; then
  echo 'Delegate registration failed';
  exit $foundRegisteredDelegate
fi
