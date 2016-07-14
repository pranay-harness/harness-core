package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

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
  private int totalCommandUnits;

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
   * Getter for property 'totalCommandUnits'.
   *
   * @return Value for property 'totalCommandUnits'.
   */
  public int getTotalCommandUnits() {
    return totalCommandUnits;
  }

  /**
   * Setter for property 'totalCommandUnits'.
   *
   * @param totalCommandUnits Value to set for property 'totalCommandUnits'.
   */
  public void setTotalCommandUnits(int totalCommandUnits) {
    this.totalCommandUnits = totalCommandUnits;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> data = super.getExecutionSummary();
    data.put("total", anExecutionDataValue().withDisplayName("Total").withValue(totalCommandUnits).build());
    return data;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "total",
        anExecutionDataValue().withDisplayName("Total").withValue(totalCommandUnits).build());
    putNotNull(
        executionDetails, "activityId", anExecutionDataValue().withDisplayName("").withValue(activityId).build());
    // putNotNull(executionDetails, "hostId", anExecutionDataValue().withDisplayName("").withValue(activityId).build());
    putNotNull(
        executionDetails, "hostName", anExecutionDataValue().withDisplayName("Host").withValue(hostName).build());
    putNotNull(executionDetails, "templateName",
        anExecutionDataValue().withDisplayName("Config").withValue(templateName).build());
    // putNotNull(executionDetails, "templateId", templateId);
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withDisplayName("Command").withValue(commandName).build());
    return executionDetails;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

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
    private int totalCommandUnits;

    private Builder() {}

    public static Builder aCommandStateExecutionData() {
      return new Builder();
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withTemplateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
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

    public Builder withTotalCommandUnits(int totalCommandUnits) {
      this.totalCommandUnits = totalCommandUnits;
      return this;
    }

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
          .withArtifactName(artifactName)
          .withTotalCommandUnits(totalCommandUnits);
    }

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
      commandStateExecutionData.setTotalCommandUnits(totalCommandUnits);
      return commandStateExecutionData;
    }
  }
}
