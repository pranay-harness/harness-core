package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.User;
import software.wings.dl.WingsDeque;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Represents State Machine Instance.
 *
 * @author Rishi
 */
@Entity(value = "stateExecutionInstances", noClassnameStored = true)
public class StateExecutionInstance extends Base {
  private String stateMachineId;
  private String stateName;
  private String stateType;
  private String contextElementType;
  private String contextElementName;

  private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
  private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();

  private StateMachineExecutionCallback callback;

  @Indexed private String executionUuid;

  @Indexed private String parentInstanceId;

  @Indexed private String prevInstanceId;

  private String nextInstanceId;

  @Indexed private String cloneInstanceId;

  private String notifyId;
  private ExecutionStatus status = ExecutionStatus.NEW;

  private Long startTs;
  private Long endTs;

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
  public ArrayDeque<ContextElement> getContextElements() {
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
   * Gets context element type.
   *
   * @return the context element type
   */
  public String getContextElementType() {
    return contextElementType;
  }

  /**
   * Sets context element type.
   *
   * @param contextElementType the context element type
   */
  public void setContextElementType(String contextElementType) {
    this.contextElementType = contextElementType;
  }

  /**
   * Gets context element name.
   *
   * @return the context element name
   */
  public String getContextElementName() {
    return contextElementName;
  }

  /**
   * Sets context element name.
   *
   * @param contextElementName the context element name
   */
  public void setContextElementName(String contextElementName) {
    this.contextElementName = contextElementName;
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
   * The Class Builder.
   */
  public static final class Builder {
    private String stateMachineId;
    private String stateName;
    private WingsDeque<ContextElement> contextElements = new WingsDeque<>();
    private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    private StateMachineExecutionCallback callback;
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
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A state execution instance.
     *
     * @return the builder
     */
    public static Builder aStateExecutionInstance() {
      return new Builder();
    }

    /**
     * With state machine id.
     *
     * @param stateMachineId the state machine id
     * @return the builder
     */
    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    /**
     * With state name.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With context elements.
     *
     * @param contextElements the context elements
     * @return the builder
     */
    public Builder withContextElements(WingsDeque<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    /**
     * With state execution map.
     *
     * @param stateExecutionMap the state execution map
     * @return the builder
     */
    public Builder withStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
      this.stateExecutionMap = stateExecutionMap;
      return this;
    }

    /**
     * With callback.
     *
     * @param callback the callback
     * @return the builder
     */
    public Builder withCallback(StateMachineExecutionCallback callback) {
      this.callback = callback;
      return this;
    }

    /**
     * With execution uuid.
     *
     * @param executionUuid the execution uuid
     * @return the builder
     */
    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    /**
     * With parent instance id.
     *
     * @param parentInstanceId the parent instance id
     * @return the builder
     */
    public Builder withParentInstanceId(String parentInstanceId) {
      this.parentInstanceId = parentInstanceId;
      return this;
    }

    /**
     * With prev instance id.
     *
     * @param prevInstanceId the prev instance id
     * @return the builder
     */
    public Builder withPrevInstanceId(String prevInstanceId) {
      this.prevInstanceId = prevInstanceId;
      return this;
    }

    /**
     * With next instance id.
     *
     * @param nextInstanceId the next instance id
     * @return the builder
     */
    public Builder withNextInstanceId(String nextInstanceId) {
      this.nextInstanceId = nextInstanceId;
      return this;
    }

    /**
     * With clone instance id.
     *
     * @param cloneInstanceId the clone instance id
     * @return the builder
     */
    public Builder withCloneInstanceId(String cloneInstanceId) {
      this.cloneInstanceId = cloneInstanceId;
      return this;
    }

    /**
     * With notify id.
     *
     * @param notifyId the notify id
     * @return the builder
     */
    public Builder withNotifyId(String notifyId) {
      this.notifyId = notifyId;
      return this;
    }

    /**
     * With status.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With start ts.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the state execution instance
     */
    public StateExecutionInstance build() {
      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setStateMachineId(stateMachineId);
      stateExecutionInstance.setStateName(stateName);
      stateExecutionInstance.setContextElements(contextElements);
      stateExecutionInstance.setStateExecutionMap(stateExecutionMap);
      stateExecutionInstance.setCallback(callback);
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
      stateExecutionInstance.setActive(active);
      return stateExecutionInstance;
    }
  }
}
