#!/usr/bin/env bash

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
   export MEMORY=4096
fi

echo "Using memory " $MEMORY

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/rest-0.0.1-SNAPSHOT-capsule.jar
fi

if [[ -z "$NEWRELIC_ENV" ]]; then
   export NEWRELIC_ENV=dev
fi

echo "Using new relic env " $NEWRELIC_ENV

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]]; then
    java -Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dnewrelic.environment=$NEWRELIC_ENV -Dfile.encoding=UTF-8 -jar $CAPSULE_JAR /opt/harness/config.yml
else
    java -Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dnewrelic.environment=$NEWRELIC_ENV -Dfile.encoding=UTF-8 -jar $CAPSULE_JAR /opt/harness/config.yml > /opt/harness/logs/portal.log 2>&1
fi
