package software.wings.beans.command;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

import java.util.Objects;

/**
 * Created by anubhaw on 2/28/17.
 */
public class CommandExecutionResult extends DelegateTaskNotifyResponseData {
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

  public CommandExecutionData getCommandExecutionData() {
    return commandExecutionData;
  }

  public void setCommandExecutionData(CommandExecutionData commandExecutionData) {
    this.commandExecutionData = commandExecutionData;
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
   * The Enum CommandExecutionStatus.
   */
  public enum CommandExecutionStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE,
    /**
     * Running execution status.
     */
    RUNNING,

    /**
     * Queued execution status.
     */
    QUEUED;

    public static CommandExecutionStatus translateExecutionStatus(ExecutionStatus executionStatus) {
      switch (executionStatus) {
        case SUCCESS:
          return SUCCESS;
        case FAILED:
          return FAILURE;
        case RUNNING:
          return RUNNING;
        case QUEUED:
          return QUEUED;
        default:
          throw new IllegalArgumentException("invalid status: " + executionStatus);
      }
    }
  }
}
