package software.wings.beans;

import software.wings.sm.ExecutionEventType;
import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/20/16.
 */
public class InstanceExecutionHistory {
  private int intanceCount;
  private String stateName;
  private ErrorCodes errorCodes;
  private ExecutionEventType executionEventType;
  private ExecutionStatus status;
  private String message;

  /**
   * Gets intance count.
   *
   * @return the intance count
   */
  public int getIntanceCount() {
    return intanceCount;
  }

  /**
   * Sets intance count.
   *
   * @param intanceCount the intance count
   */
  public void setIntanceCount(int intanceCount) {
    this.intanceCount = intanceCount;
  }

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets error codes.
   *
   * @return the error codes
   */
  public ErrorCodes getErrorCodes() {
    return errorCodes;
  }

  /**
   * Sets error codes.
   *
   * @param errorCodes the error codes
   */
  public void setErrorCodes(ErrorCodes errorCodes) {
    this.errorCodes = errorCodes;
  }

  /**
   * Gets execution event type.
   *
   * @return the execution event type
   */
  public ExecutionEventType getExecutionEventType() {
    return executionEventType;
  }

  /**
   * Sets execution event type.
   *
   * @param executionEventType the execution event type
   */
  public void setExecutionEventType(ExecutionEventType executionEventType) {
    this.executionEventType = executionEventType;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets message.
   *
   * @param message the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * The type Instance execution history builder.
   */
  public static final class InstanceExecutionHistoryBuilder {
    private int intanceCount;
    private String stateName;
    private ErrorCodes errorCodes;
    private ExecutionEventType executionEventType;
    private ExecutionStatus status;
    private String message;

    private InstanceExecutionHistoryBuilder() {}

    /**
     * An instance execution history instance execution history builder.
     *
     * @return the instance execution history builder
     */
    public static InstanceExecutionHistoryBuilder anInstanceExecutionHistory() {
      return new InstanceExecutionHistoryBuilder();
    }

    /**
     * With intance count instance execution history builder.
     *
     * @param intanceCount the intance count
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withIntanceCount(int intanceCount) {
      this.intanceCount = intanceCount;
      return this;
    }

    /**
     * With state name instance execution history builder.
     *
     * @param stateName the state name
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With error codes instance execution history builder.
     *
     * @param errorCodes the error codes
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withErrorCodes(ErrorCodes errorCodes) {
      this.errorCodes = errorCodes;
      return this;
    }

    /**
     * With execution event type instance execution history builder.
     *
     * @param executionEventType the execution event type
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withExecutionEventType(ExecutionEventType executionEventType) {
      this.executionEventType = executionEventType;
      return this;
    }

    /**
     * With status instance execution history builder.
     *
     * @param status the status
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With message instance execution history builder.
     *
     * @param message the message
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * Build instance execution history.
     *
     * @return the instance execution history
     */
    public InstanceExecutionHistory build() {
      InstanceExecutionHistory instanceExecutionHistory = new InstanceExecutionHistory();
      instanceExecutionHistory.setIntanceCount(intanceCount);
      instanceExecutionHistory.setStateName(stateName);
      instanceExecutionHistory.setErrorCodes(errorCodes);
      instanceExecutionHistory.setExecutionEventType(executionEventType);
      instanceExecutionHistory.setStatus(status);
      instanceExecutionHistory.setMessage(message);
      return instanceExecutionHistory;
    }
  }
}
