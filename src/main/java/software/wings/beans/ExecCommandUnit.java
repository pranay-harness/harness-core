package software.wings.beans;

import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;

/**
 * Created by anubhaw on 5/25/16.
 */
public class ExecCommandUnit extends CommandUnit {
  private String commandString;

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  public static final class ExecCommandUnitBuilder {
    private String commandString;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;

    private ExecCommandUnitBuilder() {}

    public static ExecCommandUnitBuilder anExecCommandUnit() {
      return new ExecCommandUnitBuilder();
    }

    public ExecCommandUnitBuilder withCommandString(String commandString) {
      this.commandString = commandString;
      return this;
    }

    public ExecCommandUnitBuilder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    public ExecCommandUnitBuilder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public ExecCommandUnitBuilder but() {
      return anExecCommandUnit()
          .withCommandString(commandString)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult);
    }

    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setCommandUnitType(commandUnitType);
      execCommandUnit.setExecutionResult(executionResult);
      return execCommandUnit;
    }
  }
}
