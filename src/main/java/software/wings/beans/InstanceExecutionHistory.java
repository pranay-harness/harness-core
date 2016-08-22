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

  public int getIntanceCount() {
    return intanceCount;
  }

  public void setIntanceCount(int intanceCount) {
    this.intanceCount = intanceCount;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public ErrorCodes getErrorCodes() {
    return errorCodes;
  }

  public void setErrorCodes(ErrorCodes errorCodes) {
    this.errorCodes = errorCodes;
  }

  public ExecutionEventType getExecutionEventType() {
    return executionEventType;
  }

  public void setExecutionEventType(ExecutionEventType executionEventType) {
    this.executionEventType = executionEventType;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public static final class InstanceExecutionHistoryBuilder {
    private int intanceCount;
    private String stateName;
    private ErrorCodes errorCodes;
    private ExecutionEventType executionEventType;
    private ExecutionStatus status;
    private String message;

    private InstanceExecutionHistoryBuilder() {}

    public static InstanceExecutionHistoryBuilder anInstanceExecutionHistory() {
      return new InstanceExecutionHistoryBuilder();
    }

    public InstanceExecutionHistoryBuilder withIntanceCount(int intanceCount) {
      this.intanceCount = intanceCount;
      return this;
    }

    public InstanceExecutionHistoryBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public InstanceExecutionHistoryBuilder withErrorCodes(ErrorCodes errorCodes) {
      this.errorCodes = errorCodes;
      return this;
    }

    public InstanceExecutionHistoryBuilder withExecutionEventType(ExecutionEventType executionEventType) {
      this.executionEventType = executionEventType;
      return this;
    }

    public InstanceExecutionHistoryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public InstanceExecutionHistoryBuilder withMessage(String message) {
      this.message = message;
      return this;
    }

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
