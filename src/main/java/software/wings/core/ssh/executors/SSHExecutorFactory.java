package software.wings.core.ssh.executors;

import software.wings.core.ssh.executors.SSHExecutor.ExecutorType;
import software.wings.exception.WingsException;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;

public class SSHExecutorFactory {
  public static SSHExecutor getExectorByType(ExecutorType executorType) {
    switch (executorType) {
      case PASSWORD:
        return new SSHPwdAuthExecutor();
      case SSHKEY:
        return new SSHPubKeyAuthExecutor();
      case JUMPBOX:
        return new SSHJumpboxExecutor();
      default:
        throw new WingsException(
            UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable("Unknown executor type: " + executorType));
    }
  }
}
