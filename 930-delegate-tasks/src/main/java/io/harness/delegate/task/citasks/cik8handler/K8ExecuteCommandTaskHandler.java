/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to execute a command or list of commands on a pod's container in a kubernetes cluster.
 */

import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.ExecuteCommandTaskHandler;
import io.harness.logging.CommandExecutionStatus;

import com.esotericsoftware.kryo.NotNull;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class K8ExecuteCommandTaskHandler implements ExecuteCommandTaskHandler {
  @NotNull private ExecuteCommandTaskHandler.Type type = ExecuteCommandTaskHandler.Type.K8;
  @Inject private K8sConnectorHelper k8sConnectorHelper;

  @Override
  public ExecuteCommandTaskHandler.Type getType() {
    return type;
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(ExecuteCommandTaskParams executeCommandTaskParams) {
    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
  }
}
