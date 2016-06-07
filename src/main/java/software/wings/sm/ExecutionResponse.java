package software.wings.sm;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Describes response of an execution.
 *
 * @author Rishi
 */
public class ExecutionResponse {
  private boolean asynch;
  private List<String> correlationIds = Lists.newArrayList();
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

  public static final class Builder {
    private boolean asynch;
    private List<String> correlationIds = Lists.newArrayList();
    private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    private String errorMessage;
    private StateExecutionData stateExecutionData;

    private Builder() {}

    public static Builder anExecutionResponse() {
      return new Builder();
    }

    public Builder withAsynch(boolean asynch) {
      this.asynch = asynch;
      return this;
    }

    public Builder addCorrelationIds(String... correlationIds) {
      this.correlationIds.addAll(asList(correlationIds));
      return this;
    }

    public Builder withCorrelationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    public Builder withExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
      return this;
    }

    public Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder withStateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionData = stateExecutionData;
      return this;
    }

    public Builder but() {
      return anExecutionResponse()
          .withAsynch(asynch)
          .withCorrelationIds(correlationIds)
          .withExecutionStatus(executionStatus)
          .withErrorMessage(errorMessage)
          .withStateExecutionData(stateExecutionData);
    }

    public ExecutionResponse build() {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setAsynch(asynch);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setExecutionStatus(executionStatus);
      executionResponse.setErrorMessage(errorMessage);
      executionResponse.setStateExecutionData(stateExecutionData);
      return executionResponse;
    }
  }
}
