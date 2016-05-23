package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;
import software.wings.beans.User;

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
  private static final long serialVersionUID = 1872770318731510631L;
  private String stateMachineId;
  private String stateName;

  @Serialized private ArrayDeque<ContextElement> contextElements = new ArrayDeque<>();
  private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();

  @Serialized private StateMachineExecutionCallback callback;

  @Indexed private String executionUuid;

  @Indexed private String parentInstanceId;

  @Indexed private String prevInstanceId;

  private String nextInstanceId;

  @Indexed private String cloneInstanceId;

  private String notifyId;
  private ExecutionStatus status = ExecutionStatus.NEW;

  private Long startTs;
  private Long endTs;

  public String getParentInstanceId() {
    return parentInstanceId;
  }

  public void setParentInstanceId(String parentInstanceId) {
    this.parentInstanceId = parentInstanceId;
  }

  public String getCloneInstanceId() {
    return cloneInstanceId;
  }

  public void setCloneInstanceId(String cloneInstanceId) {
    this.cloneInstanceId = cloneInstanceId;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public String getNotifyId() {
    return notifyId;
  }

  public void setNotifyId(String notifyId) {
    this.notifyId = notifyId;
  }

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public Long getStartTs() {
    return startTs;
  }

  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  public Long getEndTs() {
    return endTs;
  }

  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  public String getExecutionUuid() {
    return executionUuid;
  }

  public void setExecutionUuid(String executionUuid) {
    this.executionUuid = executionUuid;
  }

  public String getPrevInstanceId() {
    return prevInstanceId;
  }

  public void setPrevInstanceId(String prevInstanceId) {
    this.prevInstanceId = prevInstanceId;
  }

  public String getNextInstanceId() {
    return nextInstanceId;
  }

  public void setNextInstanceId(String nextInstanceId) {
    this.nextInstanceId = nextInstanceId;
  }

  public ArrayDeque<ContextElement> getContextElements() {
    return contextElements;
  }

  public void setContextElements(ArrayDeque<ContextElement> contextElements) {
    this.contextElements = contextElements;
  }

  public Map<String, StateExecutionData> getStateExecutionMap() {
    return stateExecutionMap;
  }

  public void setStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
    this.stateExecutionMap = stateExecutionMap;
  }

  public StateMachineExecutionCallback getCallback() {
    return callback;
  }

  public void setCallback(StateMachineExecutionCallback callback) {
    this.callback = callback;
  }

  public static final class Builder {
    private String stateMachineId;
    private String stateName;
    private ArrayDeque<ContextElement> contextElements = new ArrayDeque<>();
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

    public Builder withContextElements(ArrayDeque<ContextElement> contextElements) {
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
