package software.wings.sm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes response of an execution.
 * @author Rishi
 */
public class ExecutionResponse {
  private boolean asynch;
  private List<String> correlationIds;
  private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private String errorMessage;
  private Map<String, ? extends Serializable> response = new HashMap<>();

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

  public Map<String, ? extends Serializable> getResponse() {
    return response;
  }

  public void setResponse(Map<String, ? extends Serializable> response) {
    this.response = response;
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
