package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorConstants.UNKNOWN_EXECUTOR_TYPE_ERROR;

import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * A factory for creating SshExecutor objects.
 */
public class SshExecutorFactory {
  @Inject private FileService fileService;
  @Inject private ExecutionLogs executionLogs;

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
        executor = new SshPwdAuthExecutor(executionLogs, fileService);
        break;
      case KEY_AUTH:
        executor = new SshPubKeyAuthExecutor(executionLogs, fileService);
        break;
      case BASTION_HOST:
        executor = new SshJumpboxExecutor(executionLogs, fileService);
        break;
      default:
        throw new WingsException(UNKNOWN_EXECUTOR_TYPE_ERROR, new Throwable("Unknown executor type: " + executorType));
    }
    return executor;
  }
}
