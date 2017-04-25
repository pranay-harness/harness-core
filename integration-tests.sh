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
sleep 30

#run data gen to load test data
cd rest
mvn test -Dtest=software.wings.integration.DataGenUtil
cd ../

#run delegate
export MAVEN_OPTS="-Xbootclasspath/p:$HOME/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0"
cd delegate & nohup mvn exec:java > delegate.out 2>&1 &
echo $! > delegate.pid

#wait for delegate to start
echo 'sleep for delegate to start'
sleep 30

#run all integration tests
mvn failsafe:integration-test


