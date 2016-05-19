package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;

import software.wings.exception.WingsException;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

public class SshExecutorFactory {
  @Inject private static FileService fileService;
  @Inject private static ExecutionLogs executionLogs;

  public static SshExecutor getExecutor(SshSessionConfig config) {
    SshExecutor executor;
    switch (config.getExecutorType()) {
      case PASSWORD:
        executor = new SshPwdAuthExecutor(executionLogs, fileService);
        break;
      case SSHKEY:
        executor = new SshPubKeyAuthExecutor(executionLogs, fileService);
        break;
      case JUMPBOX:
        executor = new SshJumpboxExecutor(executionLogs, fileService);
        break;
      default:
        throw new WingsException(UNKNOWN_ERROR, new Throwable("Unknown executor type: " + config.getExecutorType()));
    }
    executor.init(config);
    return executor;
  }
}
