/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.shell;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.logging.LogCallback;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ShellExecutorConfig;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ShellExecutorFactoryNG {
  public ScriptProcessExecutor getExecutor(ShellExecutorConfig shellExecutorConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new ScriptProcessExecutor(
        getExecutionLogCallback(shellExecutorConfig, logStreamingTaskClient, commandUnitsProgress), true,
        shellExecutorConfig);
  }

  private LogCallback getExecutionLogCallback(ShellExecutorConfig shellExecutorConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(
        logStreamingTaskClient, shellExecutorConfig.getCommandUnitName(), true, commandUnitsProgress);
  }
}
