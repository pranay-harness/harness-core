package software.wings.sm;

import com.google.common.collect.Lists;

import io.harness.beans.ExecutionStatus;

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
  private String delegateTaskId;
  private List<StateExecutionInstance> stateExecutionInstanceList;

  /**
   * Gets state execution instance list.
   *
   * @return the state execution instance list
   */
  public List<StateExecutionInstance> getStateExecutionInstanceList() {
    return stateExecutionInstanceList;
  }

  /**
   * Sets state execution instance list.
   *
   * @param stateExecutionInstanceList the state execution instance list
   */
  public void setStateExecutionInstanceList(List<StateExecutionInstance> stateExecutionInstanceList) {
    this.stateExecutionInstanceList = stateExecutionInstanceList;
  }

  /**
   * Adds the.
   *
   * @param stateExecutionInstance the state execution instance
   */
  public void add(StateExecutionInstance stateExecutionInstance) {
    if (this.stateExecutionInstanceList == null) {
      this.stateExecutionInstanceList = new ArrayList<>();
    }
    this.stateExecutionInstanceList.add(stateExecutionInstance);
  }

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
   * Getter for property 'delegateTaskId'.
   *
   * @return Value for property 'delegateTaskId'.
   */
  public String getDelegateTaskId() {
    return delegateTaskId;
  }

  /**
   * Setter for property 'delegateTaskId'.
   *
   * @param delegateTaskId Value to set for property 'delegateTaskId'.
   */
  public void setDelegateTaskId(String delegateTaskId) {
    this.delegateTaskId = delegateTaskId;
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
    private List<ContextElement> notifyElements;
    private List<ContextElement> contextElements;
    private String delegateTaskId;

    private Builder() {}

    public static Builder anExecutionResponse() {
      return new Builder();
    }

    public Builder async(boolean async) {
      this.async = async;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationIds.add(correlationId);
      return this;
    }

    public Builder correlationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    public Builder executionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder stateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionData = stateExecutionData;
      return this;
    }

    public Builder notifyElements(List<ContextElement> notifyElements) {
      this.notifyElements = notifyElements;
      return this;
    }

    public Builder contextElements(List<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    public Builder contextElement(ContextElement contextElement) {
      if (this.contextElements == null) {
        this.contextElements = new ArrayList<>();
      }
      this.contextElements.add(contextElement);
      return this;
    }

    public Builder notifyElement(ContextElement notifyElement) {
      if (this.notifyElements == null) {
        this.notifyElements = new ArrayList<>();
      }
      this.notifyElements.add(notifyElement);
      return this;
    }

    public Builder delegateTaskId(String delegateTaskId) {
      this.delegateTaskId = delegateTaskId;
      return this;
    }

    public Builder but() {
      return anExecutionResponse()
          .async(async)
          .correlationIds(correlationIds)
          .executionStatus(executionStatus)
          .errorMessage(errorMessage)
          .stateExecutionData(stateExecutionData)
          .notifyElements(notifyElements)
          .contextElements(contextElements)
          .delegateTaskId(delegateTaskId);
    }

    public ExecutionResponse build() {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setAsync(async);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setExecutionStatus(executionStatus);
      executionResponse.setErrorMessage(errorMessage);
      executionResponse.setStateExecutionData(stateExecutionData);
      executionResponse.setNotifyElements(notifyElements);
      executionResponse.setContextElements(contextElements);
      executionResponse.setDelegateTaskId(delegateTaskId);
      return executionResponse;
    }
  }
}
