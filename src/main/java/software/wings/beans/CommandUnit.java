package software.wings.beans;

import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;

/**
 * Created by anubhaw on 5/25/16.
 */
public class CommandUnit {
  private String serviceId;
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public CommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  public CommandUnitType getCommandUnitType() {
    return commandUnitType;
  }

  public void setCommandUnitType(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  public ExecutionResult getExecutionResult() {
    return executionResult;
  }

  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }

  public enum CommandUnitType {
    EXEC,
    SCP,
    COMMAND,
    COPY_ARTIFACT,
    COPY_PLATFORM,
    APPLY_CONFIG,
    BACKUP,
  }
}
