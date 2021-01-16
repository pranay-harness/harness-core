package io.harness.delegate.task.shell;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGLogCallback;
import io.harness.logging.LogCallback;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.SshSessionConfig;

public class SshExecutorFactoryNG {
  public ScriptSshExecutor getExecutor(
      SshSessionConfig sshSessionConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return new ScriptSshExecutor(
        getExecutionLogCallback(sshSessionConfig, logStreamingTaskClient), true, sshSessionConfig);
  }

  private LogCallback getExecutionLogCallback(
      SshSessionConfig sshSessionConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return new NGLogCallback(logStreamingTaskClient, sshSessionConfig.getCommandUnitName());
  }
}
