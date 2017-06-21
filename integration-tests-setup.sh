#!/bin/bash
sudo service mongod restart
mvn clean install -DskipTests=true
mvn test-compile

echo 'starting server'
export MAVEN_OPTS="-Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar"
cd rest
nohup mvn exec:java > portal.log 2>&1 &
echo $! > manager.pid
cd ../
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
cd rest
mvn test -Dtest=software.wings.integration.DataGenUtil
datagen_status=$?
if [[ $datagen_status -ne 0 ]] ; then
  echo 'Datagen failed';
  exit $datagen_status
fi
cd ../

#Delegate integration test
cd rest
mvn test -Dtest=software.wings.integration.DelegateIntegrationTest
delegateIntegrationTestResult=$?
if [[ $delegateIntegrationTestResult -ne 0 ]] ; then
  echo 'Delegate integration test failed';
  exit $delegateIntegrationTestResult
fi
cd ../


#run delegate
export MAVEN_OPTS="-Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0"
cd delegate
sed -i -e 's/^doUpgrade.*/doUpgrade: false/' config-delegate.yml
nohup mvn exec:java > delegate.out 2>&1 &
echo $! > delegate.pid
cd ../

#wait for delegate to start
echo 'wait for delegate to start'

#wait for delegate to start
cd rest
mvn test -Dtest=software.wings.integration.DelegateRegistrationIntegrationTest#shouldWaitForADelegateToRegister
foundRegisteredDelegate=$?
if [[ $foundRegisteredDelegate -ne 0 ]] ; then
  echo 'Delegate registration failed';
  exit $foundRegisteredDelegate
fi
cd ../
