#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


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

export JAVA_OPTS="-Xms${MIN_MEMORY} -Xmx${MAX_MEMORY} -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc $GC_PARAMS"

JAVA_OPTS=$JAVA_OPTS" -Xbootclasspath/p:/opt/harness/alpn-boot-8.1.13.v20181017.jar"

if [[ "${ENABLE_APPDYNAMICS}" == "true" ]]; then
    mkdir /opt/harness/AppServerAgent-20.8.0.30686 && unzip AppServerAgent-20.8.0.30686.zip -d /opt/harness/AppServerAgent-20.8.0.30686
    node_name="-Dappdynamics.agent.nodeName=$(hostname)"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/AppServerAgent-20.8.0.30686/javaagent.jar -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    JAVA_OPTS="$JAVA_OPTS $node_name"
    echo "Using Appdynamics java agent"
fi

java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/ci-manager-config.yml
