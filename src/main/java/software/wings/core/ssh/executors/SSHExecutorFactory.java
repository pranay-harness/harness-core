package software.wings.core.ssh.executors;

import software.wings.core.ssh.executors.SSHExecutor.ExecutorType;
import software.wings.exception.WingsException;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;

public class SSHExecutorFactory {
  public static SSHExecutor getExecutor(SSHSessionConfig config) {
    SSHExecutor executor;
    switch (config.getExecutorType()) {
      case PASSWORD:
        executor = new SSHPwdAuthExecutor();
        break;
      case SSHKEY:
        executor = new SSHPubKeyAuthExecutor();
        break;
      case JUMPBOX:
        executor = new SSHJumpboxExecutor();
        break;
      default:
        throw new WingsException(
            UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable("Unknown executor type: " + config.getExecutorType()));
    }
    executor.init(config);
    return executor;
  }
}
