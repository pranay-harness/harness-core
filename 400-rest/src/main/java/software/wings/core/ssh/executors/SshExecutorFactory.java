package software.wings.core.ssh.executors;

import io.harness.shell.BaseScriptExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.SshSessionConfig;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A factory for creating ScriptExecutor objects.
 */
@Singleton
public class SshExecutorFactory {
  @Inject private DelegateFileManager fileService;
  @Inject private DelegateLogService logService;

  /**
   * Gets the executor.
   *
   * @param sshSessionConfig the executor type
   * @return the executor
   */
  public BaseScriptExecutor getExecutor(SshSessionConfig sshSessionConfig) {
    return new ScriptSshExecutor(getExecutionLogCallback(sshSessionConfig), true, sshSessionConfig);
  }

  public BaseScriptExecutor getExecutor(SshSessionConfig sshSessionConfig, boolean shouldSaveExecutionLogs) {
    return new ScriptSshExecutor(getExecutionLogCallback(sshSessionConfig), shouldSaveExecutionLogs, sshSessionConfig);
  }

  public FileBasedScriptExecutor getFileBasedExecutor(SshSessionConfig sshSessionConfig) {
    return new FileBasedSshScriptExecutor(
        fileService, getExecutionLogCallback(sshSessionConfig), true, sshSessionConfig);
  }

  ExecutionLogCallback getExecutionLogCallback(SshSessionConfig config) {
    return new ExecutionLogCallback(
        logService, config.getAccountId(), config.getAppId(), config.getExecutionId(), config.getCommandUnitName());
  }
}
