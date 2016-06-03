package software.wings.beans;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/25/16.
 */
public class CommandUnit {
  private String name;
  @NotNull private String serviceId;
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;

  public CommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
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

  public enum ExecutionResult { SUCCESS, FAILURE }
}
