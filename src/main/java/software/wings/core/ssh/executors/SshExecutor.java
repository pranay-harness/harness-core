package software.wings.core.ssh.executors;

import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitCommandUnit;
import software.wings.beans.command.ScpCommandUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  ExecutionResult execute(@NotNull ExecCommandUnit execCommandUnit);

  /**
   * Transfer file execution result.
   *
   * @param scpCommandUnit the copy command unit
   * @return the execution result
   */
  ExecutionResult execute(ScpCommandUnit scpCommandUnit);

  ExecutionResult execute(InitCommandUnit initCommandUnit);

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
