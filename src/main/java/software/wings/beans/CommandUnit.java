package software.wings.beans;

import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;

/**
 * Created by anubhaw on 5/25/16.
 */
public class CommandUnit {
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;

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

  public enum CommandUnitType { EXEC, SCP }
}
