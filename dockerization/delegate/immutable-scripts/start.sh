#!/bin/bash -e
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

function append_config() {
  CONFIG_KEY=$1
  CONFIG_VALUE=$2
  if [ ! -z "$CONFIG_VALUE" ] ; then
    echo "$CONFIG_KEY: $CONFIG_VALUE" >> config-delegate.yml
  fi
}

# 0. Proxy setup
source ./proxy_setup.sh

# 1. Get & execute init script if present
if [ ! -z "$INIT_SCRIPT" ]; then
  echo "#!/bin/bash -e" > init.sh
  echo "$INIT_SCRIPT" >> init.sh
fi

if [ -e init.sh ]; then
    echo "Starting initialization script for delegate"
    source ./init.sh
    if [ $? -eq 0 ];
    then
      echo "Completed executing initialization script"
    else
      echo "Error while executing initialization script. Delegate wont be started."
      exit 1
    fi
fi

# 2. Build delegate-config.yml
echo "accountId: $ACCOUNT_ID" > config-delegate.yml
echo "accountSecret: $ACCOUNT_SECRET" >> config-delegate.yml
echo "managerUrl: $MANAGER_HOST_AND_PORT/api/" >> config-delegate.yml
echo "verificationServiceUrl: $MANAGER_HOST_AND_PORT/verification/" >> config-delegate.yml
echo "cvNextGenUrl: $MANAGER_HOST_AND_PORT/cv/api/" >> config-delegate.yml
echo "heartbeatIntervalMs: 60000" >> config-delegate.yml
echo "localDiskPath: /tmp" >> config-delegate.yml
echo "maxCachedArtifacts: 2" >> config-delegate.yml
echo "pollForTasks: ${POLL_FOR_TASKS:-false}" >> config-delegate.yml
echo "doUpgrade: false" >> config-delegate.yml

append_config "grpcServiceEnabled" $GRPC_SERVICE_ENABLED
append_config "grpcServiceConnectorPort" $GRPC_SERVICE_CONNECTOR_PORT
append_config "versionCheckDisabled" $VERSION_CHECK_DISABLED
append_config "clientToolsDownloadDisabled" $CLIENT_TOOLS_DOWNLOAD_DISABLED

# 3. Start the delegate
java $JAVA_OPTS $PROXY_SYS_PROPS -Xbootclasspath/p:alpn-boot-8.1.13.v20181017.jar -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -Dcom.sun.jndi.ldap.object.disableEndpointIdentification=true -DLANG=en_US.UTF-8 -jar delegate.jar server config-delegate.yml
}
