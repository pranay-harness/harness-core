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
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.SshSessionConfig;

@OwnedBy(HarnessTeam.CDC)
public class SshExecutorFactoryNG {
  public ScriptSshExecutor getExecutor(SshSessionConfig sshSessionConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new ScriptSshExecutor(
        getExecutionLogCallback(sshSessionConfig, logStreamingTaskClient, commandUnitsProgress), true,
        sshSessionConfig);
  }

  private LogCallback getExecutionLogCallback(SshSessionConfig sshSessionConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(
        logStreamingTaskClient, sshSessionConfig.getCommandUnitName(), true, commandUnitsProgress);
  }
}
