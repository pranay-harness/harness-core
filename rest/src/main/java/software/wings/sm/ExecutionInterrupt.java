/**
 *
 */

package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The type Workflow execution event.
 *
 * @author Rishi
 */
@Entity(value = "executionInterrupts", noClassnameStored = true)
public class ExecutionInterrupt extends Base {
  @NotNull private ExecutionInterruptType executionInterruptType;

  private String envId;
  @NotNull private String executionUuid;
  private String stateExecutionInstanceId;
  private Map<String, Object> properties;

  /**
   * Gets execution event type.
   *
   * @return the execution event type
   */
  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }

  /**
   * Sets execution event type.
   *
   * @param executionInterruptType the execution event type
   */
  public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
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

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionInterruptType executionInterruptType;
    private String envId;
    private String executionUuid;
    private String stateExecutionInstanceId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A workflow execution event builder.
     *
     * @return the builder
     */
    public static Builder aWorkflowExecutionInterrupt() {
      return new Builder();
    }

    /**
     * With execution event type builder.
     *
     * @param executionInterruptType the execution event type
     * @return the builder
     */
    public Builder withExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
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
     * Build workflow execution event.
     *
     * @return the workflow execution event
     */
    public ExecutionInterrupt build() {
      ExecutionInterrupt workflowExecutionInterrupt = new ExecutionInterrupt();
      workflowExecutionInterrupt.setExecutionInterruptType(executionInterruptType);
      workflowExecutionInterrupt.setEnvId(envId);
      workflowExecutionInterrupt.setExecutionUuid(executionUuid);
      workflowExecutionInterrupt.setStateExecutionInstanceId(stateExecutionInstanceId);
      workflowExecutionInterrupt.setUuid(uuid);
      workflowExecutionInterrupt.setAppId(appId);
      workflowExecutionInterrupt.setCreatedBy(createdBy);
      workflowExecutionInterrupt.setCreatedAt(createdAt);
      workflowExecutionInterrupt.setLastUpdatedBy(lastUpdatedBy);
      workflowExecutionInterrupt.setLastUpdatedAt(lastUpdatedAt);
      return workflowExecutionInterrupt;
    }
  }
}
