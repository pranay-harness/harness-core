#!/usr/bin/env bash

mkdir -p /opt/harness/logs
touch /opt/harness/logs/ci-manager.log

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$JVM_MIN_MEMORY" ]]; then
   export MIN_MEMORY=2096m
fi

if [[ -z "$JVM_MAX_MEMORY" ]]; then
   export MAX_MEMORY=2096m
fi

if [[ -z "$COMMAND" ]]; then
   export COMMAND=server
fi

echo "Using memory " "$MEMORY"

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/ci-manager-capsule.jar
fi

export GC_PARAMS=" -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=40 -XX:MaxGCPauseMillis=1000 -Dfile.encoding=UTF-8"

export JAVA_OPTS="-Xms${MAX_MEMORY} -Xmx${MIN_MEMORY} -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc $GC_PARAMS"

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] && [[ -e /opt/harness/datadog/dd-java-agent.jar ]]; then
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/datadog/dd-java-agent.jar"
fi


if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] || [[ "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" ]]; then
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/ci-manager-config.yml
else
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/ci-manager-config.yml > /opt/harness/logs/ci-manager.log 2>&1
fi