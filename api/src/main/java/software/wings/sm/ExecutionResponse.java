package software.wings.sm;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

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
  private NotifyResponseData notifyResponseData;
  private Map<String, Object> params;

  /**
   * Is asynch boolean.
   *
   * @return the boolean
   */
  public boolean isAsync() {
    return asynch;
  }

  /**
   * Sets asynch.
   *
   * @param asynch the asynch
   */
  public void setAsync(boolean asynch) {
    this.asynch = asynch;
  }

  /**
   * Gets correlation ids.
   *
   * @return the correlation ids
   */
  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  /**
   * Sets correlation ids.
   *
   * @param correlationIds the correlation ids
   */
  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  /**
   * Gets state execution data.
   *
   * @return the state execution data
   */
  public StateExecutionData getStateExecutionData() {
    return stateExecutionData;
  }

  /**
   * Sets state execution data.
   *
   * @param stateExecutionData the state execution data
   */
  public void setStateExecutionData(StateExecutionData stateExecutionData) {
    this.stateExecutionData = stateExecutionData;
  }

  /**
   * Gets execution status.
   *
   * @return the execution status
   */
  public ExecutionStatus getExecutionStatus() {
    return executionStatus;
  }

  /**
   * Sets execution status.
   *
   * @param executionStatus the execution status
   */
  public void setExecutionStatus(ExecutionStatus executionStatus) {
    this.executionStatus = executionStatus;
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

  /**
   * Gets notify response data.
   *
   * @return the notify response data
   */
  public NotifyResponseData getNotifyResponseData() {
    return notifyResponseData;
  }

  /**
   * Sets notify response data.
   *
   * @param notifyResponseData the notify response data
   */
  public void setNotifyResponseData(NotifyResponseData notifyResponseData) {
    this.notifyResponseData = notifyResponseData;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private boolean asynch;
    private List<String> correlationIds = Lists.newArrayList();
    private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    private String errorMessage;
    private StateExecutionData stateExecutionData;

    private Builder() {}

    /**
     * An execution response builder.
     *
     * @return the builder
     */
    public static Builder anExecutionResponse() {
      return new Builder();
    }

    /**
     * With asynch builder.
     *
     * @param asynch the asynch
     * @return the builder
     */
    public Builder withAsync(boolean asynch) {
      this.asynch = asynch;
      return this;
    }

    /**
     * Add correlation ids builder.
     *
     * @param correlationIds the correlation ids
     * @return the builder
     */
    public Builder addCorrelationIds(String... correlationIds) {
      this.correlationIds.addAll(asList(correlationIds));
      return this;
    }

    /**
     * With correlation ids builder.
     *
     * @param correlationIds the correlation ids
     * @return the builder
     */
    public Builder withCorrelationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    /**
     * With execution status builder.
     *
     * @param executionStatus the execution status
     * @return the builder
     */
    public Builder withExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
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
     * With state execution data builder.
     *
     * @param stateExecutionData the state execution data
     * @return the builder
     */
    public Builder withStateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionData = stateExecutionData;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anExecutionResponse()
          .withAsync(asynch)
          .withCorrelationIds(correlationIds)
          .withExecutionStatus(executionStatus)
          .withErrorMessage(errorMessage)
          .withStateExecutionData(stateExecutionData);
    }

    /**
     * Build execution response.
     *
     * @return the execution response
     */
    public ExecutionResponse build() {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setAsync(asynch);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setExecutionStatus(executionStatus);
      executionResponse.setErrorMessage(errorMessage);
      executionResponse.setStateExecutionData(stateExecutionData);
      return executionResponse;
    }
  }
}
