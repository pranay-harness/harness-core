package software.wings.sm;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsDeque;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents State Machine Instance.
 *
 * @author Rishi
 */
@Entity(value = "stateExecutionInstances", noClassnameStored = true)
public class StateExecutionInstance extends Base {
  private String stateMachineId;
  private String childStateMachineId;
  private String stateName;
  private String stateType;
  private ContextElement contextElement;
  private boolean contextTransition;
  private boolean rollback;
  private String delegateTaskId;

  @Embedded private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
  private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
  private List<StateExecutionData> stateExecutionDataHistory = new ArrayList<>();

  private List<ExecutionEventAdvisor> executionEventAdvisors;

  private List<ContextElement> notifyElements;

  private StateMachineExecutionCallback callback;

  private String executionName;

  private WorkflowType executionType;

  @Indexed private String executionUuid;

  @Indexed private String parentInstanceId;

  @Indexed private String prevInstanceId;

  private String nextInstanceId;

  @Indexed private String cloneInstanceId;

  private String notifyId;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  private Long startTs;
  private Long endTs;
  @Indexed private Long expiryTs;

  /**
   * Gets parent instance id.
   *
   * @return the parent instance id
   */
  public String getParentInstanceId() {
    return parentInstanceId;
  }

  /**
   * Sets parent instance id.
   *
   * @param parentInstanceId the parent instance id
   */
  public void setParentInstanceId(String parentInstanceId) {
    this.parentInstanceId = parentInstanceId;
  }

  /**
   * Gets clone instance id.
   *
   * @return the clone instance id
   */
  public String getCloneInstanceId() {
    return cloneInstanceId;
  }

  /**
   * Sets clone instance id.
   *
   * @param cloneInstanceId the clone instance id
   */
  public void setCloneInstanceId(String cloneInstanceId) {
    this.cloneInstanceId = cloneInstanceId;
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
   * Gets state type.
   *
   * @return the state type
   */
  public String getStateType() {
    return stateType;
  }

  /**
   * Sets state type.
   *
   * @param stateType the state type
   */
  public void setStateType(String stateType) {
    this.stateType = stateType;
  }

  /**
   * Gets notify id.
   *
   * @return the notify id
   */
  public String getNotifyId() {
    return notifyId;
  }

  /**
   * Sets notify id.
   *
   * @param notifyId the notify id
   */
  public void setNotifyId(String notifyId) {
    this.notifyId = notifyId;
  }

  public String getChildStateMachineId() {
    return childStateMachineId;
  }

  public void setChildStateMachineId(String childStateMachineId) {
    this.childStateMachineId = childStateMachineId;
  }

  /**
   * Gets state machine id.
   *
   * @return the state machine id
   */
  public String getStateMachineId() {
    return stateMachineId;
  }

  /**
   * Sets state machine id.
   *
   * @param stateMachineId the state machine id
   */
  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public List<ExecutionEventAdvisor> getExecutionEventAdvisors() {
    return executionEventAdvisors;
  }

  public void setExecutionEventAdvisors(List<ExecutionEventAdvisor> executionEventAdvisors) {
    this.executionEventAdvisors = executionEventAdvisors;
  }

  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  public Long getExpiryTs() {
    return expiryTs;
  }

  public void setExpiryTs(Long expiryTs) {
    this.expiryTs = expiryTs;
  }

  /**
   * Gets execution uuid.
   *
   * @return the execution uuid
   */
  public String getExecutionUuid() {
    return executionUuid;
  }

  /**
   * Sets execution uuid.
   *
   * @param executionUuid the execution uuid
   */
  public void setExecutionUuid(String executionUuid) {
    this.executionUuid = executionUuid;
  }

  /**
   * Gets prev instance id.
   *
   * @return the prev instance id
   */
  public String getPrevInstanceId() {
    return prevInstanceId;
  }

  /**
   * Sets prev instance id.
   *
   * @param prevInstanceId the prev instance id
   */
  public void setPrevInstanceId(String prevInstanceId) {
    this.prevInstanceId = prevInstanceId;
  }

  /**
   * Gets next instance id.
   *
   * @return the next instance id
   */
  public String getNextInstanceId() {
    return nextInstanceId;
  }

  /**
   * Sets next instance id.
   *
   * @param nextInstanceId the next instance id
   */
  public void setNextInstanceId(String nextInstanceId) {
    this.nextInstanceId = nextInstanceId;
  }

  /**
   * Gets context elements.
   *
   * @return the context elements
   */
  public WingsDeque<ContextElement> getContextElements() {
    return contextElements;
  }

  /**
   * Sets context elements.
   *
   * @param contextElements the context elements
   */
  public void setContextElements(WingsDeque<ContextElement> contextElements) {
    this.contextElements = contextElements;
  }

  /**
   * Gets state execution map.
   *
   * @return the state execution map
   */
  public Map<String, StateExecutionData> getStateExecutionMap() {
    return stateExecutionMap;
  }

  /**
   * Sets state execution map.
   *
   * @param stateExecutionMap the state execution map
   */
  public void setStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
    this.stateExecutionMap = stateExecutionMap;
  }

  /**
   * Gets callback.
   *
   * @return the callback
   */
  public StateMachineExecutionCallback getCallback() {
    return callback;
  }

  /**
   * Sets callback.
   *
   * @param callback the callback
   */
  public void setCallback(StateMachineExecutionCallback callback) {
    this.callback = callback;
  }

  /**
   * Gets context element.
   *
   * @return the context element
   */
  public ContextElement getContextElement() {
    return contextElement;
  }

  /**
   * Sets context element.
   *
   * @param contextElement the context element
   */
  public void setContextElement(ContextElement contextElement) {
    this.contextElement = contextElement;
  }

  /**
   * Gets state execution data.
   *
   * @return the state execution data
   */
  public StateExecutionData getStateExecutionData() {
    return stateExecutionMap.get(stateName);
  }

  /**
   * Is context transition boolean.
   *
   * @return the boolean
   */
  public boolean isContextTransition() {
    return contextTransition;
  }

  /**
   * Sets context transition.
   *
   * @param contextTransition the context transition
   */
  public void setContextTransition(boolean contextTransition) {
    this.contextTransition = contextTransition;
  }

  /**
   * Getter for property 'executionName'.
   *
   * @return Value for property 'executionName'.
   */
  public String getExecutionName() {
    return executionName;
  }

  /**
   * Setter for property 'executionName'.
   *
   * @param executionName Value to set for property 'executionName'.
   */
  public void setExecutionName(String executionName) {
    this.executionName = executionName;
  }

  /**
   * Gets state execution data history.
   *
   * @return the state execution data history
   */
  public List<StateExecutionData> getStateExecutionDataHistory() {
    return stateExecutionDataHistory;
  }

  /**
   * Sets state execution data history.
   *
   * @param stateExecutionDataHistory the state execution data history
   */
  public void setStateExecutionDataHistory(List<StateExecutionData> stateExecutionDataHistory) {
    this.stateExecutionDataHistory = stateExecutionDataHistory;
  }

  /**
   * Gets execution type.
   *
   * @return the execution type
   */
  public WorkflowType getExecutionType() {
    return executionType;
  }

  /**
   * Sets execution type.
   *
   * @param executionType the execution type
   */
  public void setExecutionType(WorkflowType executionType) {
    this.executionType = executionType;
  }

  public List<ContextElement> getNotifyElements() {
    return notifyElements;
  }

  public void setNotifyElements(List<ContextElement> notifyElements) {
    this.notifyElements = notifyElements;
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("stateMachineId", stateMachineId)
        .add("childStateMachineId", childStateMachineId)
        .add("stateName", stateName)
        .add("stateType", stateType)
        .add("contextElement", contextElement)
        .add("contextTransition", contextTransition)
        .add("rollback", rollback)
        .add("delegateTaskId", delegateTaskId)
        .add("contextElements", contextElements)
        .add("stateExecutionMap", stateExecutionMap)
        .add("stateExecutionDataHistory", stateExecutionDataHistory)
        .add("executionEventAdvisors", executionEventAdvisors)
        .add("notifyElements", notifyElements)
        .add("callback", callback)
        .add("executionName", executionName)
        .add("executionType", executionType)
        .add("executionUuid", executionUuid)
        .add("parentInstanceId", parentInstanceId)
        .add("prevInstanceId", prevInstanceId)
        .add("nextInstanceId", nextInstanceId)
        .add("cloneInstanceId", cloneInstanceId)
        .add("notifyId", notifyId)
        .add("status", status)
        .add("startTs", startTs)
        .add("endTs", endTs)
        .add("expiryTs", expiryTs)
        .add("stateExecutionData", getStateExecutionData())
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String stateMachineId;
    private String childStateMachineId;
    private String stateName;
    private String stateType;
    private ContextElement contextElement;
    private boolean contextTransition;
    private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
    private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    private List<StateExecutionData> stateExecutionDataHistory = new ArrayList<>();
    private StateMachineExecutionCallback callback;
    private String executionName;
    private WorkflowType executionType;
    private String executionUuid;
    private String parentInstanceId;
    private String prevInstanceId;
    private String nextInstanceId;
    private String cloneInstanceId;
    private String notifyId;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private Long startTs;
    private Long endTs;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A state execution instance builder.
     *
     * @return the builder
     */
    public static Builder aStateExecutionInstance() {
      return new Builder();
    }

    /**
     * With state machine id builder.
     *
     * @param childStateMachineId the state machine id
     * @return the builder
     */
    public Builder withChildStateMachineId(String childStateMachineId) {
      this.childStateMachineId = childStateMachineId;
      return this;
    }

    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With state type builder.
     *
     * @param stateType the state type
     * @return the builder
     */
    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    /**
     * With context element builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    /**
     * With context transition builder.
     *
     * @param contextTransition the context transition
     * @return the builder
     */
    public Builder withContextTransition(boolean contextTransition) {
      this.contextTransition = contextTransition;
      return this;
    }

    /**
     * With context elements builder.
     *
     * @param contextElements the context elements
     * @return the builder
     */
    public Builder withContextElements(WingsDeque<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    /**
     * With context elements builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public Builder addContextElement(ContextElement contextElement) {
      this.contextElements.add(contextElement);
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateName the state name
     * @param stateExecutionData the state execution data
     * @return the builder
     */
    public Builder addStateExecutionData(String stateName, StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(stateName, stateExecutionData);
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateExecutionMap the state execution map
     * @return the builder
     */
    public Builder withStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
      this.stateExecutionMap = stateExecutionMap;
      return this;
    }

    /**
     * With state execution map builder.
     *
     * @param stateExecutionData the state execution data
     * @return the builder
     */
    public Builder addStateExecutionData(StateExecutionData stateExecutionData) {
      this.stateExecutionMap.put(stateName, stateExecutionData);
      return this;
    }

    /**
     * With state execution data history builder.
     *
     * @param stateExecutionDataHistory the state execution data history
     * @return the builder
     */
    public Builder withStateExecutionDataHistory(List<StateExecutionData> stateExecutionDataHistory) {
      this.stateExecutionDataHistory = stateExecutionDataHistory;
      return this;
    }

    /**
     * With callback builder.
     *
     * @param callback the callback
     * @return the builder
     */
    public Builder withCallback(StateMachineExecutionCallback callback) {
      this.callback = callback;
      return this;
    }

    /**
     * With execution name builder.
     *
     * @param executionName the execution name
     * @return the builder
     */
    public Builder withExecutionName(String executionName) {
      this.executionName = executionName;
      return this;
    }

    /**
     * With execution type builder.
     *
     * @param executionType the execution type
     * @return the builder
     */
    public Builder withExecutionType(WorkflowType executionType) {
      this.executionType = executionType;
      return this;
    }

    /**
     * With execution uuid builder.
     *
     * @param executionUuid the execution uuid
     * @return the builder
     */
    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    /**
     * With parent instance id builder.
     *
     * @param parentInstanceId the parent instance id
     * @return the builder
     */
    public Builder withParentInstanceId(String parentInstanceId) {
      this.parentInstanceId = parentInstanceId;
      return this;
    }

    /**
     * With prev instance id builder.
     *
     * @param prevInstanceId the prev instance id
     * @return the builder
     */
    public Builder withPrevInstanceId(String prevInstanceId) {
      this.prevInstanceId = prevInstanceId;
      return this;
    }

    /**
     * With next instance id builder.
     *
     * @param nextInstanceId the next instance id
     * @return the builder
     */
    public Builder withNextInstanceId(String nextInstanceId) {
      this.nextInstanceId = nextInstanceId;
      return this;
    }

    /**
     * With clone instance id builder.
     *
     * @param cloneInstanceId the clone instance id
     * @return the builder
     */
    public Builder withCloneInstanceId(String cloneInstanceId) {
      this.cloneInstanceId = cloneInstanceId;
      return this;
    }

    /**
     * With notify id builder.
     *
     * @param notifyId the notify id
     * @return the builder
     */
    public Builder withNotifyId(String notifyId) {
      this.notifyId = notifyId;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aStateExecutionInstance()
          .withStateMachineId(stateMachineId)
          .withStateName(stateName)
          .withStateType(stateType)
          .withContextElement(contextElement)
          .withContextTransition(contextTransition)
          .withContextElements(contextElements)
          .withStateExecutionMap(stateExecutionMap)
          .withStateExecutionDataHistory(stateExecutionDataHistory)
          .withCallback(callback)
          .withExecutionName(executionName)
          .withExecutionType(executionType)
          .withExecutionUuid(executionUuid)
          .withParentInstanceId(parentInstanceId)
          .withPrevInstanceId(prevInstanceId)
          .withNextInstanceId(nextInstanceId)
          .withCloneInstanceId(cloneInstanceId)
          .withNotifyId(notifyId)
          .withStatus(status)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build state execution instance.
     *
     * @return the state execution instance
     */
    public StateExecutionInstance build() {
      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setStateMachineId(stateMachineId);
      stateExecutionInstance.setChildStateMachineId(childStateMachineId);
      stateExecutionInstance.setStateName(stateName);
      stateExecutionInstance.setStateType(stateType);
      stateExecutionInstance.setContextElement(contextElement);
      stateExecutionInstance.setContextTransition(contextTransition);
      stateExecutionInstance.setContextElements(contextElements);
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
      stateExecutionInstance.setStateExecutionDataHistory(stateExecutionDataHistory);
      stateExecutionInstance.setCallback(callback);
      stateExecutionInstance.setExecutionName(executionName);
      stateExecutionInstance.setExecutionType(executionType);
      stateExecutionInstance.setExecutionUuid(executionUuid);
      stateExecutionInstance.setParentInstanceId(parentInstanceId);
      stateExecutionInstance.setPrevInstanceId(prevInstanceId);
      stateExecutionInstance.setNextInstanceId(nextInstanceId);
      stateExecutionInstance.setCloneInstanceId(cloneInstanceId);
      stateExecutionInstance.setNotifyId(notifyId);
      stateExecutionInstance.setStatus(status);
      stateExecutionInstance.setStartTs(startTs);
      stateExecutionInstance.setEndTs(endTs);
      stateExecutionInstance.setUuid(uuid);
      stateExecutionInstance.setAppId(appId);
      stateExecutionInstance.setCreatedBy(createdBy);
      stateExecutionInstance.setCreatedAt(createdAt);
      stateExecutionInstance.setLastUpdatedBy(lastUpdatedBy);
      stateExecutionInstance.setLastUpdatedAt(lastUpdatedAt);
      return stateExecutionInstance;
    }
  }
}
