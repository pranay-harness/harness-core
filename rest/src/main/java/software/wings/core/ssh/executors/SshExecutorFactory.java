package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorCodes.UNKNOWN_EXECUTOR_TYPE_ERROR;

import com.google.inject.Singleton;

import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;

import javax.inject.Inject;

/**
 * A factory for creating SshExecutor objects.
 */
@Singleton
public class SshExecutorFactory {
  @Inject private DelegateFileManager fileService;
  @Inject private DelegateLogService logService;

  /**
   * Gets the executor.
   *
   * @param executorType the executor type
   * @return the executor
   */
  public SshExecutor getExecutor(ExecutorType executorType) {
    SshExecutor executor;
    switch (executorType) {
      case PASSWORD_AUTH:
        executor = new SshPwdAuthExecutor(fileService, logService);
        break;
      case KEY_AUTH:
        executor = new SshPubKeyAuthExecutor(fileService, logService);
        break;
      case BASTION_HOST:
        executor = new SshJumpboxExecutor(fileService, logService);
        break;
      default:
        throw new WingsException(UNKNOWN_EXECUTOR_TYPE_ERROR, new Throwable("Unknown executor type: " + executorType));
    }
    return executor;
  }
}
