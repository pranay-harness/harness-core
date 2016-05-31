package software.wings.beans;

import static software.wings.beans.CommandUnit.CommandUnitType.EXEC;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 5/25/16.
 */
public class ExecCommandUnit extends CommandUnit {
  @NotEmpty private String commandPath;

  @NotEmpty private String commandString;

  public ExecCommandUnit() {
    super(EXEC);
  }

  public String getCommandPath() {
    return commandPath;
  }

  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("commandPath", commandPath)
        .add("commandString", commandString)
        .toString();
  }

  public static final class Builder {
    private String commandPath;
    private String commandString;
    private String serviceId;
    private ExecutionResult executionResult;

    private Builder() {}

    public static Builder anExecCommandUnit() {
      return new Builder();
    }

    public Builder withCommandPath(String commandPath) {
      this.commandPath = commandPath;
      return this;
    }

    public Builder withCommandString(String commandString) {
      this.commandString = commandString;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public Builder but() {
      return anExecCommandUnit()
          .withCommandPath(commandPath)
          .withCommandString(commandString)
          .withServiceId(serviceId)
          .withExecutionResult(executionResult);
    }

    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandPath(commandPath);
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setServiceId(serviceId);
      execCommandUnit.setExecutionResult(executionResult);
      return execCommandUnit;
    }
  }
}
