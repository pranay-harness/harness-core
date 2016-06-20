package software.wings.api;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
public class CommandStateExecutionData extends StateExecutionData {
  private static final long serialVersionUID = 8379244073342534217L;

  private String hostName;
  private String hostId;
  private String commandName;
  private String serviceName;
  private String serviceId;
  private String templateName;
  private String templateId;
  private String activityId;
  private String artifactId;
  private String artifactName;

  /**
   * Getter for property 'hostName'.
   *
   * @return Value for property 'hostName'.
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Setter for property 'hostName'.
   *
   * @param hostName Value to set for property 'hostName'.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Getter for property 'hostId'.
   *
   * @return Value for property 'hostId'.
   */
  public String getHostId() {
    return hostId;
  }

  /**
   * Setter for property 'hostId'.
   *
   * @param hostId Value to set for property 'hostId'.
   */
  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  /**
   * Getter for property 'commandName'.
   *
   * @return Value for property 'commandName'.
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Setter for property 'commandName'.
   *
   * @param commandName Value to set for property 'commandName'.
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Getter for property 'serviceName'.
   *
   * @return Value for property 'serviceName'.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Setter for property 'serviceName'.
   *
   * @param serviceName Value to set for property 'serviceName'.
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Getter for property 'serviceId'.
   *
   * @return Value for property 'serviceId'.
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Setter for property 'serviceId'.
   *
   * @param serviceId Value to set for property 'serviceId'.
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Getter for property 'templateName'.
   *
   * @return Value for property 'templateName'.
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * Setter for property 'templateName'.
   *
   * @param templateName Value to set for property 'templateName'.
   */
  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  /**
   * Getter for property 'templateId'.
   *
   * @return Value for property 'templateId'.
   */
  public String getTemplateId() {
    return templateId;
  }

  /**
   * Setter for property 'templateId'.
   *
   * @param templateId Value to set for property 'templateId'.
   */
  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  /**
   * Getter for property 'activityId'.
   *
   * @return Value for property 'activityId'.
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Setter for property 'activityId'.
   *
   * @param activityId Value to set for property 'activityId'.
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Getter for property 'artifactId'.
   *
   * @return Value for property 'artifactId'.
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Setter for property 'artifactId'.
   *
   * @param artifactId Value to set for property 'artifactId'.
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /**
   * Getter for property 'artifactName'.
   *
   * @return Value for property 'artifactName'.
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Setter for property 'artifactName'.
   *
   * @param artifactName Value to set for property 'artifactName'.
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String hostName;
    private String hostId;
    private String commandName;
    private String stateName;
    private String serviceName;
    private Long startTs;
    private String serviceId;
    private Long endTs;
    private String templateName;
    private ExecutionStatus status;
    private String templateId;
    private String activityId;
    private String artifactId;
    private String artifactName;

    private Builder() {}

    /**
     * A command state execution data builder.
     *
     * @return the builder
     */
    public static Builder aCommandStateExecutionData() {
      return new Builder();
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With host id builder.
     *
     * @param hostId the host id
     * @return the builder
     */
    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    /**
     * With command name builder.
     *
     * @param commandName the command name
     * @return the builder
     */
    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
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
     * With service name builder.
     *
     * @param serviceName the service name
     * @return the builder
     */
    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
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
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
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
     * With template name builder.
     *
     * @param templateName the template name
     * @return the builder
     */
    public Builder withTemplateName(String templateName) {
      this.templateName = templateName;
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
     * With template id builder.
     *
     * @param templateId the template id
     * @return the builder
     */
    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    /**
     * With activity id builder.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With artifact id builder.
     *
     * @param artifactId the artifact id
     * @return the builder
     */
    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    /**
     * With artifact name builder.
     *
     * @param artifactName the artifact name
     * @return the builder
     */
    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommandStateExecutionData()
          .withHostName(hostName)
          .withHostId(hostId)
          .withCommandName(commandName)
          .withStateName(stateName)
          .withServiceName(serviceName)
          .withStartTs(startTs)
          .withServiceId(serviceId)
          .withEndTs(endTs)
          .withTemplateName(templateName)
          .withStatus(status)
          .withTemplateId(templateId)
          .withActivityId(activityId)
          .withArtifactId(artifactId)
          .withArtifactName(artifactName);
    }

    /**
     * Build command state execution data.
     *
     * @return the command state execution data
     */
    public CommandStateExecutionData build() {
      CommandStateExecutionData commandStateExecutionData = new CommandStateExecutionData();
      commandStateExecutionData.setHostName(hostName);
      commandStateExecutionData.setHostId(hostId);
      commandStateExecutionData.setCommandName(commandName);
      commandStateExecutionData.setStateName(stateName);
      commandStateExecutionData.setServiceName(serviceName);
      commandStateExecutionData.setStartTs(startTs);
      commandStateExecutionData.setServiceId(serviceId);
      commandStateExecutionData.setEndTs(endTs);
      commandStateExecutionData.setTemplateName(templateName);
      commandStateExecutionData.setStatus(status);
      commandStateExecutionData.setTemplateId(templateId);
      commandStateExecutionData.setActivityId(activityId);
      commandStateExecutionData.setArtifactId(artifactId);
      commandStateExecutionData.setArtifactName(artifactName);
      return commandStateExecutionData;
    }
  }
}
