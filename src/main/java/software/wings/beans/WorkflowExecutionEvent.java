/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import software.wings.sm.ExecutionEventType;

/**
 * @author Rishi
 *
 */
@Entity(value = "workflowExecutionEvent", noClassnameStored = true)
public class WorkflowExecutionEvent extends Base {
  private ExecutionEventType executionEventType;

  private String envId;
  private String workflowExecutionId;
  private String stateExecutionInstanceId;

  public ExecutionEventType getExecutionEventType() {
    return executionEventType;
  }

  public void setExecutionEventType(ExecutionEventType executionEventType) {
    this.executionEventType = executionEventType;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public static final class Builder {
    private ExecutionEventType executionEventType;
    private String envId;
    private String workflowExecutionId;
    private String stateExecutionInstanceId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aWorkflowExecutionEvent() {
      return new Builder();
    }

    public Builder withExecutionEventType(ExecutionEventType executionEventType) {
      this.executionEventType = executionEventType;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
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

    public WorkflowExecutionEvent build() {
      WorkflowExecutionEvent workflowExecutionEvent = new WorkflowExecutionEvent();
      workflowExecutionEvent.setExecutionEventType(executionEventType);
      workflowExecutionEvent.setEnvId(envId);
      workflowExecutionEvent.setWorkflowExecutionId(workflowExecutionId);
      workflowExecutionEvent.setStateExecutionInstanceId(stateExecutionInstanceId);
      workflowExecutionEvent.setUuid(uuid);
      workflowExecutionEvent.setAppId(appId);
      workflowExecutionEvent.setCreatedBy(createdBy);
      workflowExecutionEvent.setCreatedAt(createdAt);
      workflowExecutionEvent.setLastUpdatedBy(lastUpdatedBy);
      workflowExecutionEvent.setLastUpdatedAt(lastUpdatedAt);
      workflowExecutionEvent.setActive(active);
      return workflowExecutionEvent;
    }
  }
}
