package io.harness.delegate.task.shell;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGLogCallback;
import io.harness.logging.LogCallback;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ShellExecutorConfig;

import com.google.inject.Singleton;

@Singleton
public class ShellExecutorFactoryNG {
  public ScriptProcessExecutor getExecutor(
      ShellExecutorConfig shellExecutorConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return new ScriptProcessExecutor(
        getExecutionLogCallback(shellExecutorConfig, logStreamingTaskClient), true, shellExecutorConfig);
  }

  private LogCallback getExecutionLogCallback(
      ShellExecutorConfig shellExecutorConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return new NGLogCallback(logStreamingTaskClient, shellExecutorConfig.getCommandUnitName());
  }
}
