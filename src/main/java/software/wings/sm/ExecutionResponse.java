package software.wings.sm;

import java.util.List;

/**
 * Describes response of an execution.
 * @author Rishi
 */
public class ExecutionResponse {
  private boolean asynch;
  private List<String> correlationIds;
  private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private String errorMessage;
  private StateExecutionData stateExecutionData;

  public boolean isAsynch() {
    return asynch;
  }

  public void setAsynch(boolean asynch) {
    this.asynch = asynch;
  }

  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  public StateExecutionData getStateExecutionData() {
    return stateExecutionData;
  }

  public void setStateExecutionData(StateExecutionData stateExecutionData) {
    this.stateExecutionData = stateExecutionData;
  }

  public ExecutionStatus getExecutionStatus() {
    return executionStatus;
  }

  public void setExecutionStatus(ExecutionStatus executionStatus) {
    this.executionStatus = executionStatus;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
