#!/usr/bin/env bash

mkdir -p /opt/harness/logs
touch /opt/harness/logs/event-server.log

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
   export MEMORY=4096
fi

echo "Using memory " $MEMORY

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/event-server-capsule.jar
fi

export JAVA_OPTS="-Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8"

JAVA_OPTS=$JAVA_OPTS" -Xbootclasspath/p:/opt/harness/alpn-boot-8.1.13.v20181017.jar"

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] || [[ "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" ]]; then
    java $JAVA_OPTS -jar $CAPSULE_JAR server /opt/harness/event-service-config.yml
else
    java $JAVA_OPTS -jar $CAPSULE_JAR server /opt/harness/event-service-config.yml > /opt/harness/logs/event-server.log 2>&1
fi
