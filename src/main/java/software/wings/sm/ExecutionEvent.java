/**
 *
 */

package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.User;

import javax.validation.constraints.NotNull;

/**
 * The type Workflow execution event.
 *
 * @author Rishi
 */
@Entity(value = "executionEvents", noClassnameStored = true)
public class ExecutionEvent extends Base {
  @NotNull private ExecutionEventType executionEventType;

  @NotNull private String envId;
  @NotNull private String executionUuid;
  private String stateExecutionInstanceId;

  /**
   * Gets execution event type.
   *
   * @return the execution event type
   */
  public ExecutionEventType getExecutionEventType() {
    return executionEventType;
  }

  /**
   * Sets execution event type.
   *
   * @param executionEventType the execution event type
   */
  public void setExecutionEventType(ExecutionEventType executionEventType) {
    this.executionEventType = executionEventType;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getExecutionUuid() {
    return executionUuid;
  }

  public void setExecutionUuid(String executionUuid) {
    this.executionUuid = executionUuid;
  }

  /**
   * Gets state execution instance id.
   *
   * @return the state execution instance id
   */
  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  /**
   * Sets state execution instance id.
   *
   * @param stateExecutionInstanceId the state execution instance id
   */
  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionEventType executionEventType;
    private String envId;
    private String executionUuid;
    private String stateExecutionInstanceId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A workflow execution event builder.
     *
     * @return the builder
     */
    public static Builder aWorkflowExecutionEvent() {
      return new Builder();
    }

    /**
     * With execution event type builder.
     *
     * @param executionEventType the execution event type
     * @return the builder
     */
    public Builder withExecutionEventType(ExecutionEventType executionEventType) {
      this.executionEventType = executionEventType;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With executionUuid builder.
     *
     * @param executionUuid the workflow execution id
     * @return the builder
     */
    public Builder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    /**
     * With state execution instance id builder.
     *
     * @param stateExecutionInstanceId the state execution instance id
     * @return the builder
     */
    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
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
    public Builder withCreatedBy(User createdBy) {
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
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
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
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * Build workflow execution event.
     *
     * @return the workflow execution event
     */
    public ExecutionEvent build() {
      ExecutionEvent workflowExecutionEvent = new ExecutionEvent();
      workflowExecutionEvent.setExecutionEventType(executionEventType);
      workflowExecutionEvent.setEnvId(envId);
      workflowExecutionEvent.setExecutionUuid(executionUuid);
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
