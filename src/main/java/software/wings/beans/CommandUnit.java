package software.wings.beans;

import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/25/16.
 */
public class CommandUnit {
  private String name;
  @NotNull private String serviceId;
  @NotNull private String appId;
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;
  private boolean artifactNeeded;

  /**
   * Instantiates a new Command unit.
   */
  public CommandUnit() {}

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public CommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets command unit type.
   *
   * @return the command unit type
   */
  public CommandUnitType getCommandUnitType() {
    return commandUnitType;
  }

  /**
   * Sets command unit type.
   *
   * @param commandUnitType the command unit type
   */
  public void setCommandUnitType(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Gets execution result.
   *
   * @return the execution result
   */
  public ExecutionResult getExecutionResult() {
    return executionResult;
  }

  /**
   * Sets execution result.
   *
   * @param executionResult the execution result
   */
  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Is artifact needed boolean.
   *
   * @return the boolean
   */
  public boolean isArtifactNeeded() {
    return artifactNeeded;
  }

  /**
   * Sets artifact needed.
   *
   * @param artifactNeeded the artifact needed
   */
  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }

  /**
   * The Enum ExecutionResult.
   */
  public enum ExecutionResult {
    /**
     * Success execution result.
     */
    SUCCESS, /**
              * Failure execution result.
              */
    FAILURE
  }
}
