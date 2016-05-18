package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;

import software.wings.exception.WingsException;

public class SshExecutorFactory {
  public static SshExecutor getExecutor(SshSessionConfig config) {
    SshExecutor executor;
    switch (config.getExecutorType()) {
      case PASSWORD:
        executor = new SshPwdAuthExecutor();
        break;
      case SSHKEY:
        executor = new SshPubKeyAuthExecutor();
        break;
      case JUMPBOX:
        executor = new SshJumpboxExecutor();
        break;
      default:
        throw new WingsException(UNKNOWN_ERROR, new Throwable("Unknown executor type: " + config.getExecutorType()));
    }
    executor.init(config);
    return executor;
  }
}
