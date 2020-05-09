#!/usr/bin/env bash

mkdir -p /opt/harness/logs
touch /opt/harness/logs/command-library-server.log

if [[ -v "{hostname}" ]]; then
  export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
  export MEMORY=4096
fi

echo "Using memory " $MEMORY

if [[ -z "$CAPSULE_JAR" ]]; then
  export CAPSULE_JAR=/opt/harness/command-library-app-capsule.jar
fi

export JAVA_OPTS="-Xms${MEMORY}m -Xmx${MEMORY}m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8"

if [[ "${ENABLE_APPDYNAMICS}" == "true" ]]; then
  tar -xvzf AppServerAgent-4.5.0.23604.tar.gz
  node_name="-Dappdynamics.agent.nodeName=$(hostname)"
  JAVA_OPTS=$JAVA_OPTS" -Dcapsule.jvm.args=-javaagent:/opt/harness/AppServerAgent-4.5.0.23604/javaagent.jar -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
  JAVA_OPTS="$JAVA_OPTS $node_name"
  echo "Using Appdynamics java agent"
fi

if [[ "${DEPLOY_MODE}" == "KUBERNETES" ]] || [[ "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" ]]; then
  java $JAVA_OPTS -jar $CAPSULE_JAR /opt/harness/command-library-server-config.yml
else
  java $JAVA_OPTS -jar $CAPSULE_JAR /opt/harness/command-library-server-config.yml >/opt/harness/logs/command-library-server.log 2>&1
fi
