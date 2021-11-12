#!/usr/bin/env bash

mkdir -p /opt/harness/logs
touch /opt/harness/logs/ng-dashboard-service.log

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
   export MEMORY=4096
fi

echo "Using memory " $MEMORY

if [[ -z "$COMMAND" ]]; then
   export COMMAND=server
fi

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/ng-dashboard-service.jar
fi

if [[ "${ENABLE_G1GC}" == "true" ]]; then
    export GC_PARAMS="-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=40 -XX:MaxGCPauseMillis=1000 -Dfile.encoding=UTF-8"
else
    export GC_PARAMS="-XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8"
fi

export JAVA_OPTS="-Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc $GC_PARAMS"

if [[ "${ENABLE_APPDYNAMICS}" == "true" ]]; then
    echo "enable_appD=true"
    mkdir /opt/harness/AppServerAgent-20.8.0.30686 && unzip AppServerAgent-20.8.0.30686.zip -d /opt/harness/AppServerAgent-20.8.0.30686
    echo "unzip complete"
    node_name="-Dappdynamics.agent.nodeName=$(hostname)"
    echo "node name added"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/AppServerAgent-20.8.0.30686/javaagent.jar -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    JAVA_OPTS="$JAVA_OPTS $node_name"
    echo "Using Appdynamics java agent"
fi

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] || [[ "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" ]]; then
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/config.yml
else
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/config.yml > /opt/harness/logs/ng-dashboard-service.log 2>&1
fi
