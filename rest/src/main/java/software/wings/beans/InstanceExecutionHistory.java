package software.wings.beans;

import io.harness.eraro.ErrorCode;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/20/16.
 */
public class InstanceExecutionHistory {
  private int intanceCount;
  private String stateName;
  private ErrorCode errorCode;
  private ExecutionInterruptType executionInterruptType;
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
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Sets error codes.
   *
   * @param errorCode the error codes
   */
  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Gets execution event type.
   *
   * @return the execution event type
   */
  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }

  /**
   * Sets execution event type.
   *
   * @param executionInterruptType the execution event type
   */
  public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
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
    private ErrorCode errorCode;
    private ExecutionInterruptType executionInterruptType;
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
     * @param errorCode the error codes
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withErrorCodes(ErrorCode errorCode) {
      this.errorCode = errorCode;
      return this;
    }

    /**
     * With execution event type instance execution history builder.
     *
     * @param executionInterruptType the execution event type
     * @return the instance execution history builder
     */
    public InstanceExecutionHistoryBuilder withExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
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
      instanceExecutionHistory.setErrorCode(errorCode);
      instanceExecutionHistory.setExecutionInterruptType(executionInterruptType);
      instanceExecutionHistory.setStatus(status);
      instanceExecutionHistory.setMessage(message);
      return instanceExecutionHistory;
    }
  }
}
