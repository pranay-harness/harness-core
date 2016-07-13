package software.wings.core.ssh.executors;

import software.wings.beans.AbstractExecCommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.CopyCommandUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor {
  /**
   * Inits the.
   *
   * @param config the config
   */
  void init(@Valid SshSessionConfig config);

  /**
   * Execute execution result.
   *
   * @param execCommandUnit the exec command unit
   * @return the execution result
   */
  ExecutionResult execute(@NotNull AbstractExecCommandUnit execCommandUnit);

  /**
   * Transfer file execution result.
   *
   * @param copyCommandUnit the copy command unit
   * @return the execution result
   */
  ExecutionResult transferFile(CopyCommandUnit copyCommandUnit);

  /**
   * Abort.
   */
  void abort();

  /**
   * Destroy.
   */
  void destroy();

  /**
   * The Enum ExecutorType.
   */
  enum ExecutorType {
    /**
     * Password auth executor type.
     */
    PASSWORD_AUTH, /**
                    * Key auth executor type.
                    */
    KEY_AUTH, /**
               * Bastion host executor type.
               */
    BASTION_HOST
  }
}
