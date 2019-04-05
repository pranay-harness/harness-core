package software.wings.core.ssh.executors;

import software.wings.core.BaseScriptExecutor;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface ScriptExecutor extends BaseScriptExecutor {
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
