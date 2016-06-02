package software.wings.beans;

import static org.joor.Reflect.on;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/25/16.
 */
public class CommandUnit {
  private String name;
  private String executionId;
  @NotNull private String serviceId;
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;

  public CommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public enum CommandUnitType {
    EXEC(ExecCommandUnit.class),
    COMMAND(Command.class),
    COPY_ARTIFACT(CopyArtifactCommandUnit.class),
    ;

    private Class<? extends CommandUnit> commandUnitClass;

    private CommandUnitType(Class<? extends CommandUnit> commandUnitClass) {
      this.commandUnitClass = commandUnitClass;
    }
    public CommandUnit newInstance() {
      return on(commandUnitClass).create().get();
    }
  }

  public enum ExecutionResult { SUCCESS, FAILURE }
}
