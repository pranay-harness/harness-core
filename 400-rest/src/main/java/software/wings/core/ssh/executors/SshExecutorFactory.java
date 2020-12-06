package software.wings.core.ssh.executors;

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
  public ScriptExecutor getExecutor(SshSessionConfig sshSessionConfig) {
    return new ScriptSshExecutor(fileService, logService, true, sshSessionConfig);
  }

  public ScriptExecutor getExecutor(SshSessionConfig sshSessionConfig, boolean shouldSaveExecutionLogs) {
    return new ScriptSshExecutor(fileService, logService, shouldSaveExecutionLogs, sshSessionConfig);
  }
}
