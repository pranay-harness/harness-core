package software.wings.core.ssh.executors;

import software.wings.core.BaseExecutor;

import javax.validation.Valid;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor extends BaseExecutor {
  /**
   * Inits the.
   *
   * @param config the config
   */
  void init(@Valid SshSessionConfig config);

  /**
   * The Enum ExecutorType.
   */
  enum ExecutorType {
    /**
     * Password auth executor type.
     */
    PASSWORD_AUTH,
    /**
     * Key auth executor type.
     */
    KEY_AUTH,
    /**
     * Bastion host executor type.
     */
    BASTION_HOST
  }
}
