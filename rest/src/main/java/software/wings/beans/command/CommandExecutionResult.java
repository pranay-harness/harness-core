package software.wings.beans.command;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.beans.command.CommandExecutionResult.AbstractCommandUnit.CommandExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Objects;

/**
 * Created by anubhaw on 2/28/17.
 */
public class CommandExecutionResult implements NotifyResponseData {
  private CommandExecutionStatus status;
  private CommandExecutionData commandExecutionData;
  private String errorMessage;

  /**
   * Instantiates a new Execution status data.
   */
  public CommandExecutionResult() {}

  /**
   * Gets status.
   *
   * @return the status
   */
  public CommandExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(CommandExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets error message.
   *
   * @return the error message
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets error message.
   *
   * @param errorMessage the error message
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, commandExecutionData, errorMessage);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CommandExecutionResult other = (CommandExecutionResult) obj;
    return Objects.equals(this.status, other.status)
        && Objects.equals(this.commandExecutionData, other.commandExecutionData)
        && Objects.equals(this.errorMessage, other.errorMessage);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("commandExecutionData", commandExecutionData)
        .add("errorMessage", errorMessage)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private CommandExecutionStatus status;
    private CommandExecutionData commandExecutionData;
    private String errorMessage;

    private Builder() {}

    /**
     * A command execution result builder.
     *
     * @return the builder
     */
    public static Builder aCommandExecutionResult() {
      return new Builder();
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(CommandExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With command execution data builder.
     *
     * @param commandExecutionData the command execution data
     * @return the builder
     */
    public Builder withCommandExecutionData(CommandExecutionData commandExecutionData) {
      this.commandExecutionData = commandExecutionData;
      return this;
    }

    /**
     * With error message builder.
     *
     * @param errorMessage the error message
     * @return the builder
     */
    public Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommandExecutionResult()
          .withStatus(status)
          .withCommandExecutionData(commandExecutionData)
          .withErrorMessage(errorMessage);
    }

    /**
     * Build command execution result.
     *
     * @return the command execution result
     */
    public CommandExecutionResult build() {
      CommandExecutionResult commandExecutionResult = new CommandExecutionResult();
      commandExecutionResult.setStatus(status);
      commandExecutionResult.setErrorMessage(errorMessage);
      commandExecutionResult.commandExecutionData = this.commandExecutionData;
      return commandExecutionResult;
    }
  }

  /**
   * Created by anubhaw on 5/25/16.
   */
  public abstract static class AbstractCommandUnit implements CommandUnit {
    @SchemaIgnore private String name;
    private CommandUnitType commandUnitType;
    private CommandExecutionStatus commandExecutionStatus;
    @SchemaIgnore private boolean artifactNeeded = false;
    @SchemaIgnore private String deploymentType;

    /**
     * Instantiates a new Command unit.
     */
    public AbstractCommandUnit() {}

    /**
     * Instantiates a new command unit.
     *
     * @param commandUnitType the command unit type
     */
    public AbstractCommandUnit(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
    }

    /**
     * Gets command unit type.
     *
     * @return the command unit type
     */
    @Override
    @SchemaIgnore
    public CommandUnitType getCommandUnitType() {
      return commandUnitType;
    }

    /**
     * Sets command unit type.
     *
     * @param commandUnitType the command unit type
     */
    @Override
    public void setCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
    }

    /**
     * Gets execution status.
     *
     * @return the execution status
     */
    @SchemaIgnore
    public CommandExecutionStatus getCommandExecutionStatus() {
      return commandExecutionStatus;
    }

    /**
     * Sets execution status.
     *
     * @param commandExecutionStatus the execution status
     */
    public void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
      this.commandExecutionStatus = commandExecutionStatus;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    @Override
    @SchemaIgnore
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    @Override
    @SchemaIgnore
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Is artifact needed boolean.
     *
     * @return the boolean
     */
    @Override
    @SchemaIgnore
    public boolean isArtifactNeeded() {
      return artifactNeeded;
    }

    /**
     * Sets artifact needed.
     *
     * @param artifactNeeded the artifact needed
     */
    @Override
    public void setArtifactNeeded(boolean artifactNeeded) {
      this.artifactNeeded = artifactNeeded;
    }

    @Override
    @SchemaIgnore
    public String getDeploymentType() {
      return deploymentType;
    }

    /**
     * Sets deployment type.
     *
     * @param deploymentType the deployment type
     */
    public void setDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("name", name)
          .add("commandUnitType", commandUnitType)
          .add("commandExecutionStatus", commandExecutionStatus)
          .add("artifactNeeded", artifactNeeded)
          .toString();
    }

    /**
     * The enum Command unit execution status.
     */
    public enum CommandUnitExecutionResult {
      /**
       * Stop command unit execution status.
       */
      STOP, /**
             * Continue command unit execution status.
             */
      CONTINUE;

      private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;

      /**
       * Gets execution status.
       *
       * @return the execution status
       */
      public CommandExecutionStatus getCommandExecutionStatus() {
        return commandExecutionStatus;
      }

      /**
       * Sets execution status.
       *
       * @param commandExecutionStatus the execution status
       */
      public void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
        this.commandExecutionStatus = commandExecutionStatus;
      }
    }

    /**
     * The Enum CommandExecutionStatus.
     */
    public enum CommandExecutionStatus {
      /**
       * Success execution status.
       */
      SUCCESS, /**
                * Failure execution status.
                */
      FAILURE, /**
                * Running execution status.
                */
      RUNNING,

      /**
       * Queued execution status.
       */
      QUEUED
    }
  }
}
