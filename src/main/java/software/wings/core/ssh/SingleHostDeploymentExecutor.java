package software.wings.core.ssh;

import software.wings.core.ssh.executors.SSHExecutor;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SSHExecutorFactory;
import software.wings.core.ssh.executors.SSHSessionConfig;

import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.SUCCESS;

/**
 * Created by anubhaw on 2/23/16.
 */
public class SingleHostDeploymentExecutor {
  private String setupCommand;
  private String localFilePath;
  private String remoteFilePath;
  private String deployCommand;
  private SSHSessionConfig config;

  public SingleHostDeploymentExecutor(
      String setupCommand, String localFilePath, String remoteFilePath, String deployCommand, SSHSessionConfig config) {
    this.setupCommand = setupCommand;
    this.localFilePath = localFilePath;
    this.remoteFilePath = remoteFilePath;
    this.deployCommand = deployCommand;
    this.config = config;
  }

  public ExecutionResult deploy() {
    SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
    ExecutionResult result = executor.execute(setupCommand);
    ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), "Setup finished with " + result);

    if (SUCCESS == result) {
      executor = SSHExecutorFactory.getExecutor(config);
      result = executor.transferFile(localFilePath, remoteFilePath);
      ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), "File transfer finished with " + result);

      if (SUCCESS == result) {
        executor = SSHExecutorFactory.getExecutor(config);
        result = executor.execute(deployCommand);
        ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), "Deploy command finished with " + result);
      }
    }
    return result;
  }
}
