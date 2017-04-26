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
cd ../

#run delegate
export MAVEN_OPTS="-Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0"
cd delegate
nohup mvn exec:java > delegate.out 2>&1 &
echo $! > delegate.pid
cd ../

#wait for delegate to start
echo 'sleep for delegate to start'
sleep 30

#run all integration tests
mvn failsafe:integration-test failsafe:verify

#take dump of mongodb
mongodump

#tar.gz dump files
tar -czvf dump.tar.gz dump/
