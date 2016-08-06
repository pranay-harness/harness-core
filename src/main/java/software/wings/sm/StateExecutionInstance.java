package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.User;
import software.wings.dl.WingsDeque;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

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
  private boolean contextTransition;

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

  @Override
  public String toString() {
    return "StateExecutionInstance [stateMachineId=" + stateMachineId + ", stateName=" + stateName + ", stateType="
        + stateType + ", contextElementType=" + contextElementType + ", contextElementName=" + contextElementName
        + ", contextTransition=" + contextTransition + ", contextElements=" + contextElements
        + ", stateExecutionMap=" + stateExecutionMap + ", callback=" + callback + ", executionUuid=" + executionUuid
        + ", parentInstanceId=" + parentInstanceId + ", prevInstanceId=" + prevInstanceId
        + ", nextInstanceId=" + nextInstanceId + ", cloneInstanceId=" + cloneInstanceId + ", notifyId=" + notifyId
        + ", status=" + status + ", startTs=" + startTs + ", endTs=" + endTs + "]";
  }

  public static final class Builder {
    private String stateMachineId;
    private String stateName;
    private String stateType;
    private String contextElementType;
    private String contextElementName;
    private boolean contextTransition;
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

    public static Builder aStateExecutionInstance() {
      return new Builder();
    }

    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public Builder withContextElementType(String contextElementType) {
      this.contextElementType = contextElementType;
      return this;
    }

    public Builder withContextElementName(String contextElementName) {
      this.contextElementName = contextElementName;
      return this;
    }

    public Builder withContextTransition(boolean contextTransition) {
      this.contextTransition = contextTransition;
      return this;
    }

    public Builder withContextElements(WingsDeque<ContextElement> contextElements) {
      this.contextElements = contextElements;
      return this;
    }

    public Builder withStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
      this.stateExecutionMap = stateExecutionMap;
      return this;
    }

    public Builder withCallback(StateMachineExecutionCallback callback) {
      this.callback = callback;
      return this;
    }

    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    public Builder withParentInstanceId(String parentInstanceId) {
      this.parentInstanceId = parentInstanceId;
      return this;
    }

    public Builder withPrevInstanceId(String prevInstanceId) {
      this.prevInstanceId = prevInstanceId;
      return this;
    }

    public Builder withNextInstanceId(String nextInstanceId) {
      this.nextInstanceId = nextInstanceId;
      return this;
    }

    public Builder withCloneInstanceId(String cloneInstanceId) {
      this.cloneInstanceId = cloneInstanceId;
      return this;
    }

    public Builder withNotifyId(String notifyId) {
      this.notifyId = notifyId;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public StateExecutionInstance build() {
      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setStateMachineId(stateMachineId);
      stateExecutionInstance.setStateName(stateName);
      stateExecutionInstance.setStateType(stateType);
      stateExecutionInstance.setContextElementType(contextElementType);
      stateExecutionInstance.setContextElementName(contextElementName);
      stateExecutionInstance.setContextTransition(contextTransition);
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
