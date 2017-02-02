package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.sm.ExecutionStatus;

import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "activities", noClassnameStored = true)
public class Activity extends Base {
  private Type type;
  @NotEmpty private String applicationName;
  @NotEmpty private String environmentId;
  @NotEmpty private String environmentName;
  @NotNull private EnvironmentType environmentType;
  @NotEmpty private String commandName;
  private Map<String, Integer> commandNameVersionMap;
  private Map<String, String> serviceVariables = Maps.newHashMap();
  private String commandType;
  private String serviceId;
  private String serviceName;
  private String serviceTemplateId;
  private String serviceTemplateName;
  private String hostName;
  private String serviceInstanceId;
  @NotEmpty private String workflowExecutionId;
  @NotEmpty private String workflowExecutionName;
  @NotNull private WorkflowType workflowType;
  @NotEmpty private String stateExecutionInstanceId;
  @NotEmpty private String stateExecutionInstanceName;

  private String artifactStreamId;
  private String artifactStreamName;
  private boolean isPipeline;
  private String artifactId;
  private String artifactName;
  private ExecutionStatus status = ExecutionStatus.RUNNING;

  /**
   * Gets application name.
   *
   * @return the application name
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * Sets application name.
   *
   * @param applicationName the application name
   */
  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * Gets environment id.
   *
   * @return the environment id
   */
  public String getEnvironmentId() {
    return environmentId;
  }

  /**
   * Sets environment id.
   *
   * @param environmentId the environment id
   */
  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets command type.
   *
   * @return the command type
   */
  public String getCommandType() {
    return commandType;
  }

  /**
   * Sets command type.
   *
   * @param commandType the command type
   */
  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets service name.
   *
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets service name.
   *
   * @param serviceName the service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets service template id.
   *
   * @return the service template id
   */
  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  /**
   * Sets service template id.
   *
   * @param serviceTemplateId the service template id
   */
  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  /**
   * Gets service template name.
   *
   * @return the service template name
   */
  public String getServiceTemplateName() {
    return serviceTemplateName;
  }

  /**
   * Sets service template name.
   *
   * @param serviceTemplateName the service template name
   */
  public void setServiceTemplateName(String serviceTemplateName) {
    this.serviceTemplateName = serviceTemplateName;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets release id.
   *
   * @return the release id
   */
  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  /**
   * Sets release id.
   *
   * @param artifactStreamId the release id
   */
  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  /**
   * Gets release name.
   *
   * @return the release name
   */
  public String getArtifactStreamName() {
    return artifactStreamName;
  }

  /**
   * Sets release name.
   *
   * @param artifactStreamName the release name
   */
  public void setArtifactStreamName(String artifactStreamName) {
    this.artifactStreamName = artifactStreamName;
  }

  /**
   * Gets artifact name.
   *
   * @return the artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Sets artifact name.
   *
   * @param artifactName the artifact name
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
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
   * Gets environment name.
   *
   * @return the environment name
   */
  public String getEnvironmentName() {
    return environmentName;
  }

  /**
   * Sets environment name.
   *
   * @param environmentName the environment name
   */
  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }

  /**
   * Gets environment type.
   *
   * @return the environment type
   */
  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  /**
   * Sets environment type.
   *
   * @param environmentType the environment type
   */
  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  /**
   * Gets artifact id.
   *
   * @return the artifact id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Sets artifact id.
   *
   * @param artifactId the artifact id
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /**
   * Getter for property 'type'.
   *
   * @return Value for property 'type'.
   */
  public Type getType() {
    return type;
  }

  /**
   * Setter for property 'type'.
   *
   * @param type Value to set for property 'type'.
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Getter for property 'workflowExecutionId'.
   *
   * @return Value for property 'workflowExecutionId'.
   */
  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  /**
   * Setter for property 'workflowExecutionId'.
   *
   * @param workflowExecutionId Value to set for property 'workflowExecutionId'.
   */
  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  /**
   * Getter for property 'workflowExecutionName'.
   *
   * @return Value for property 'workflowExecutionName'.
   */
  public String getWorkflowExecutionName() {
    return workflowExecutionName;
  }

  /**
   * Setter for property 'workflowExecutionName'.
   *
   * @param workflowExecutionName Value to set for property 'workflowExecutionName'.
   */
  public void setWorkflowExecutionName(String workflowExecutionName) {
    this.workflowExecutionName = workflowExecutionName;
  }

  /**
   * Getter for property 'stateExecutionInstanceId'.
   *
   * @return Value for property 'stateExecutionInstanceId'.
   */
  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  /**
   * Setter for property 'stateExecutionInstanceId'.
   *
   * @param stateExecutionInstanceId Value to set for property 'stateExecutionInstanceId'.
   */
  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  /**
   * Getter for property 'stateExecutionInstanceName'.
   *
   * @return Value for property 'stateExecutionInstanceName'.
   */
  public String getStateExecutionInstanceName() {
    return stateExecutionInstanceName;
  }

  /**
   * Setter for property 'stateExecutionInstanceName'.
   *
   * @param stateExecutionInstanceName Value to set for property 'stateExecutionInstanceName'.
   */
  public void setStateExecutionInstanceName(String stateExecutionInstanceName) {
    this.stateExecutionInstanceName = stateExecutionInstanceName;
  }

  /**
   * Getter for property 'pipeline'.
   *
   * @return Value for property 'pipeline'.
   */
  public boolean isPipeline() {
    return isPipeline;
  }

  /**
   * Setter for property 'pipeline'.
   *
   * @param pipeline Value to set for property 'pipeline'.
   */
  public void setPipeline(boolean pipeline) {
    isPipeline = pipeline;
  }

  /**
   * Gets service instance id.
   *
   * @return the service instance id
   */
  public String getServiceInstanceId() {
    return serviceInstanceId;
  }

  /**
   * Sets service instance id.
   *
   * @param serviceInstanceId the service instance id
   */
  public void setServiceInstanceId(String serviceInstanceId) {
    this.serviceInstanceId = serviceInstanceId;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  public Map<String, Integer> getCommandNameVersionMap() {
    return commandNameVersionMap;
  }

  public void setCommandNameVersionMap(Map<String, Integer> commandNameVersionMap) {
    this.commandNameVersionMap = commandNameVersionMap;
  }

  /**
   * Getter for property 'serviceVariables'.
   *
   * @return Value for property 'serviceVariables'.
   */
  public Map<String, String> getServiceVariables() {
    return serviceVariables;
  }

  /**
   * Setter for property 'serviceVariables'.
   *
   * @param serviceVariables Value to set for property 'serviceVariables'.
   */
  public void setServiceVariables(Map<String, String> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(type, applicationName, environmentId, environmentName, environmentType, commandName, commandType,
              serviceId, serviceName, serviceTemplateId, serviceTemplateName, hostName, serviceInstanceId,
              workflowExecutionId, workflowExecutionName, workflowType, stateExecutionInstanceId,
              stateExecutionInstanceName, artifactStreamId, artifactStreamName, isPipeline, artifactId, artifactName,
              status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Activity other = (Activity) obj;
    return Objects.equals(this.type, other.type) && Objects.equals(this.applicationName, other.applicationName)
        && Objects.equals(this.environmentId, other.environmentId)
        && Objects.equals(this.environmentName, other.environmentName)
        && Objects.equals(this.environmentType, other.environmentType)
        && Objects.equals(this.commandName, other.commandName) && Objects.equals(this.commandType, other.commandType)
        && Objects.equals(this.serviceId, other.serviceId) && Objects.equals(this.serviceName, other.serviceName)
        && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.serviceTemplateName, other.serviceTemplateName)
        && Objects.equals(this.hostName, other.hostName)
        && Objects.equals(this.serviceInstanceId, other.serviceInstanceId)
        && Objects.equals(this.workflowExecutionId, other.workflowExecutionId)
        && Objects.equals(this.workflowExecutionName, other.workflowExecutionName)
        && Objects.equals(this.workflowType, other.workflowType)
        && Objects.equals(this.stateExecutionInstanceId, other.stateExecutionInstanceId)
        && Objects.equals(this.stateExecutionInstanceName, other.stateExecutionInstanceName)
        && Objects.equals(this.artifactStreamId, other.artifactStreamId)
        && Objects.equals(this.artifactStreamName, other.artifactStreamName)
        && Objects.equals(this.isPipeline, other.isPipeline) && Objects.equals(this.artifactId, other.artifactId)
        && Objects.equals(this.artifactName, other.artifactName) && Objects.equals(this.status, other.status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", type)
        .add("applicationName", applicationName)
        .add("environmentId", environmentId)
        .add("environmentName", environmentName)
        .add("environmentType", environmentType)
        .add("commandName", commandName)
        .add("commandType", commandType)
        .add("serviceId", serviceId)
        .add("serviceName", serviceName)
        .add("serviceTemplateId", serviceTemplateId)
        .add("serviceTemplateName", serviceTemplateName)
        .add("hostName", hostName)
        .add("serviceInstanceId", serviceInstanceId)
        .add("workflowExecutionId", workflowExecutionId)
        .add("workflowExecutionName", workflowExecutionName)
        .add("workflowType", workflowType)
        .add("stateExecutionInstanceId", stateExecutionInstanceId)
        .add("stateExecutionInstanceName", stateExecutionInstanceName)
        .add("artifactStreamId", artifactStreamId)
        .add("artifactStreamName", artifactStreamName)
        .add("isPipeline", isPipeline)
        .add("artifactId", artifactId)
        .add("artifactName", artifactName)
        .add("status", status)
        .toString();
  }

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Command type.
     */
    Command, /**
              * Verification type.
              */
    Verification
  }

  public static final class Builder {
    private Type type;
    private String applicationName;
    private String environmentId;
    private String environmentName;
    private EnvironmentType environmentType;
    private String commandName;
    private Map<String, Integer> commandNameVersionMap;
    private Map<String, String> serviceVariables = Maps.newHashMap();
    private String commandType;
    private String serviceId;
    private String serviceName;
    private String serviceTemplateId;
    private String serviceTemplateName;
    private String hostName;
    private String serviceInstanceId;
    private String workflowExecutionId;
    private String workflowExecutionName;
    private WorkflowType workflowType;
    private String uuid;
    private String stateExecutionInstanceId;
    private String appId;
    private EmbeddedUser createdBy;
    private String stateExecutionInstanceName;
    private long createdAt;
    private String artifactStreamId;
    private EmbeddedUser lastUpdatedBy;
    private String artifactStreamName;
    private long lastUpdatedAt;
    private boolean isPipeline;
    private String artifactId;
    private String artifactName;
    private ExecutionStatus status = ExecutionStatus.RUNNING;

    private Builder() {}

    public static Builder anActivity() {
      return new Builder();
    }

    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    public Builder withApplicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    public Builder withEnvironmentName(String environmentName) {
      this.environmentName = environmentName;
      return this;
    }

    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withCommandNameVersionMap(Map<String, Integer> commandNameVersionMap) {
      this.commandNameVersionMap = commandNameVersionMap;
      return this;
    }

    public Builder withCommandType(String commandType) {
      this.commandType = commandType;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceTemplateName(String serviceTemplateName) {
      this.serviceTemplateName = serviceTemplateName;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withServiceInstanceId(String serviceInstanceId) {
      this.serviceInstanceId = serviceInstanceId;
      return this;
    }

    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    public Builder withWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
      return this;
    }

    public Builder withWorkflowExecutionName(String workflowExecutionName) {
      this.workflowExecutionName = workflowExecutionName;
      return this;
    }

    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withStateExecutionInstanceName(String stateExecutionInstanceName) {
      this.stateExecutionInstanceName = stateExecutionInstanceName;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withArtifactStreamName(String artifactStreamName) {
      this.artifactStreamName = artifactStreamName;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withIsPipeline(boolean isPipeline) {
      this.isPipeline = isPipeline;
      return this;
    }

    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder but() {
      return anActivity()
          .withType(type)
          .withApplicationName(applicationName)
          .withEnvironmentId(environmentId)
          .withEnvironmentName(environmentName)
          .withEnvironmentType(environmentType)
          .withCommandName(commandName)
          .withCommandNameVersionMap(commandNameVersionMap)
          .withCommandType(commandType)
          .withServiceId(serviceId)
          .withServiceName(serviceName)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceTemplateName(serviceTemplateName)
          .withHostName(hostName)
          .withServiceInstanceId(serviceInstanceId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withServiceVariables(serviceVariables)
          .withWorkflowExecutionName(workflowExecutionName)
          .withWorkflowType(workflowType)
          .withUuid(uuid)
          .withStateExecutionInstanceId(stateExecutionInstanceId)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withStateExecutionInstanceName(stateExecutionInstanceName)
          .withCreatedAt(createdAt)
          .withArtifactStreamId(artifactStreamId)
          .withLastUpdatedBy(lastUpdatedBy)
          .withArtifactStreamName(artifactStreamName)
          .withLastUpdatedAt(lastUpdatedAt)
          .withIsPipeline(isPipeline)
          .withArtifactId(artifactId)
          .withArtifactName(artifactName)
          .withStatus(status);
    }

    public Activity build() {
      Activity activity = new Activity();
      activity.setType(type);
      activity.setApplicationName(applicationName);
      activity.setEnvironmentId(environmentId);
      activity.setEnvironmentName(environmentName);
      activity.setEnvironmentType(environmentType);
      activity.setCommandName(commandName);
      activity.setCommandNameVersionMap(commandNameVersionMap);
      activity.setServiceVariables(serviceVariables);
      activity.setCommandType(commandType);
      activity.setServiceId(serviceId);
      activity.setServiceName(serviceName);
      activity.setServiceTemplateId(serviceTemplateId);
      activity.setServiceTemplateName(serviceTemplateName);
      activity.setHostName(hostName);
      activity.setServiceInstanceId(serviceInstanceId);
      activity.setWorkflowExecutionId(workflowExecutionId);
      activity.setWorkflowExecutionName(workflowExecutionName);
      activity.setWorkflowType(workflowType);
      activity.setUuid(uuid);
      activity.setStateExecutionInstanceId(stateExecutionInstanceId);
      activity.setAppId(appId);
      activity.setCreatedBy(createdBy);
      activity.setStateExecutionInstanceName(stateExecutionInstanceName);
      activity.setCreatedAt(createdAt);
      activity.setArtifactStreamId(artifactStreamId);
      activity.setLastUpdatedBy(lastUpdatedBy);
      activity.setArtifactStreamName(artifactStreamName);
      activity.setLastUpdatedAt(lastUpdatedAt);
      activity.setArtifactId(artifactId);
      activity.setArtifactName(artifactName);
      activity.setStatus(status);
      activity.isPipeline = this.isPipeline;
      return activity;
    }
  }
}
