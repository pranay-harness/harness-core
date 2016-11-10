package software.wings.api;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.command.CommandUnit;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
public class CommandStateExecutionData extends StateExecutionData {
  private String appId;
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
  private CountsByStatuses countsByStatuses;

  @Transient @Inject private transient ActivityService activityService;

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
   * Getter for property 'countsByStatuses'.
   *
   * @return Value for property 'countsByStatuses'.
   */
  public CountsByStatuses getCountsByStatuses() {
    return countsByStatuses;
  }

  /**
   * Setter for property 'countsByStatuses'.
   *
   * @param countsByStatuses Value to set for property 'countsByStatuses'.
   */
  public void setCountsByStatuses(CountsByStatuses countsByStatuses) {
    this.countsByStatuses = countsByStatuses;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> data = super.getExecutionSummary();
    if (isNotEmpty(appId) && isNotEmpty(activityId) && activityService != null) {
      if (countsByStatuses == null) {
        List<CommandUnit> commandUnits = activityService.getCommandUnits(appId, activityId);
        countsByStatuses = new CountsByStatuses();
        commandUnits.stream().forEach(commandUnit -> {
          switch (commandUnit.getExecutionResult()) {
            case SUCCESS:
              countsByStatuses.setSuccess(countsByStatuses.getSuccess() + 1);
              break;
            case FAILURE:
              countsByStatuses.setFailed(countsByStatuses.getFailed() + 1);
              break;
            case RUNNING:
              countsByStatuses.setInprogress(countsByStatuses.getInprogress() + 1);
              break;
            case QUEUED:
              countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
              break;
          }
        });
      }
      data.put("total",
          anExecutionDataValue()
              .withDisplayName("Total")
              .withValue(countsByStatuses.getFailed() + countsByStatuses.getInprogress() + countsByStatuses.getSuccess()
                  + countsByStatuses.getQueued())
              .build());
      data.put("breakdown", anExecutionDataValue().withDisplayName("breakdown").withValue(countsByStatuses).build());
    }
    putNotNull(data, "hostName", anExecutionDataValue().withDisplayName("Host").withValue(hostName).build());
    putNotNull(data, "templateName", anExecutionDataValue().withDisplayName("Config").withValue(templateName).build());
    putNotNull(data, "commandName", anExecutionDataValue().withDisplayName("Command").withValue(commandName).build());

    return data;
  }

  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "hostName", anExecutionDataValue().withDisplayName("Host").withValue(hostName).build());
    putNotNull(executionDetails, "templateName",
        anExecutionDataValue().withDisplayName("Config").withValue(templateName).build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withDisplayName("Command").withValue(commandName).build());
    putNotNull(
        executionDetails, "activityId", anExecutionDataValue().withDisplayName("").withValue(activityId).build());
    return executionDetails;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String stateName;
    private String appId;
    private Long startTs;
    private String hostName;
    private Long endTs;
    private String hostId;
    private ExecutionStatus status;
    private String commandName;
    private String errorMsg;
    private String serviceName;
    private String serviceId;
    private String templateName;
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
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
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
          .withStateName(stateName)
          .withAppId(appId)
          .withStartTs(startTs)
          .withHostName(hostName)
          .withEndTs(endTs)
          .withHostId(hostId)
          .withStatus(status)
          .withCommandName(commandName)
          .withErrorMsg(errorMsg)
          .withServiceName(serviceName)
          .withServiceId(serviceId)
          .withTemplateName(templateName)
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
      commandStateExecutionData.setStateName(stateName);
      commandStateExecutionData.setAppId(appId);
      commandStateExecutionData.setStartTs(startTs);
      commandStateExecutionData.setHostName(hostName);
      commandStateExecutionData.setEndTs(endTs);
      commandStateExecutionData.setHostId(hostId);
      commandStateExecutionData.setStatus(status);
      commandStateExecutionData.setCommandName(commandName);
      commandStateExecutionData.setErrorMsg(errorMsg);
      commandStateExecutionData.setServiceName(serviceName);
      commandStateExecutionData.setServiceId(serviceId);
      commandStateExecutionData.setTemplateName(templateName);
      commandStateExecutionData.setTemplateId(templateId);
      commandStateExecutionData.setActivityId(activityId);
      commandStateExecutionData.setArtifactId(artifactId);
      commandStateExecutionData.setArtifactName(artifactName);
      return commandStateExecutionData;
    }
  }
}
