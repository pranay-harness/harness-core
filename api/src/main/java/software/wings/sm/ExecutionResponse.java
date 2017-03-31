package software.wings.sm;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes response of an execution.
 *
 * @author Rishi
 */
public class ExecutionResponse {
  private boolean async;
  private List<String> correlationIds = Lists.newArrayList();
  private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private String errorMessage;
  private StateExecutionData stateExecutionData;
  private List<ContextElement> notifyElements;
  private List<ContextElement> contextElements;

  /**
   * Is async boolean.
   *
   * @return the boolean
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * Sets async.
   *
   * @param asynch the async
   */
  public void setAsync(boolean asynch) {
    this.async = asynch;
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

  public List<ContextElement> getNotifyElements() {
    return notifyElements;
  }

  public void setNotifyElements(List<ContextElement> notifyElements) {
    this.notifyElements = notifyElements;
  }

  public List<ContextElement> getContextElements() {
    return contextElements;
  }

  public void setContextElements(List<ContextElement> contextElements) {
    this.contextElements = contextElements;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private boolean async;
    private List<String> correlationIds = Lists.newArrayList();
    private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    private String errorMessage;
    private StateExecutionData stateExecutionData;
    private List<ContextElement> contextElements;
    private List<ContextElement> notifyElements;

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
     * With async builder.
     *
     * @param async the async
     * @return the builder
     */
    public Builder withAsync(boolean async) {
      this.async = async;
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
     * Add param.
     *
     * @param contextElement the contextElement
     * @return the builder
     */
    public Builder addContextElement(ContextElement contextElement) {
      if (this.contextElements == null) {
        this.contextElements = new ArrayList<>();
      }
      this.contextElements.add(contextElement);
      return this;
    }

    /**
     * Add param.
     *
     * @param notifyElement the contextElement to notify
     * @return the builder
     */
    public Builder addNotifyElement(ContextElement notifyElement) {
      if (this.notifyElements == null) {
        this.notifyElements = new ArrayList<>();
      }
      this.notifyElements.add(notifyElement);
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anExecutionResponse()
          .withAsync(async)
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
      executionResponse.setAsync(async);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setExecutionStatus(executionStatus);
      executionResponse.setErrorMessage(errorMessage);
      executionResponse.setStateExecutionData(stateExecutionData);
      executionResponse.setContextElements(contextElements);
      executionResponse.setNotifyElements(notifyElements);
      return executionResponse;
    }
  }
}
