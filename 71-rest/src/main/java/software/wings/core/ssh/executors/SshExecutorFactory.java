package software.wings.core.ssh.executors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

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
    return new ScriptSshExecutor(fileService, logService, sshSessionConfig);
  }
}
