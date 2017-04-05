package software.wings.api;

import static com.google.common.base.Strings.emptyToNull;
import static org.apache.commons.lang.ArrayUtils.getLength;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rishi on 2/15/17.
 */
public class EcsServiceExecutionData extends StateExecutionData implements NotifyResponseData {
  private String ecsClusterName;
  private String ecsServiceName;
  private String ecsOldServiceName;
  private String dockerImageName;
  private String commandName;
  private int instanceCount;
  private String loadBalancerName;
  private String targetGroupArn;
  private String roleArn;

  public String getEcsClusterName() {
    return ecsClusterName;
  }

  public void setEcsClusterName(String ecsClusterName) {
    this.ecsClusterName = ecsClusterName;
  }

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
  }

  public String getEcsOldServiceName() {
    return ecsOldServiceName;
  }

  public void setEcsOldServiceName(String ecsOldServiceName) {
    this.ecsOldServiceName = ecsOldServiceName;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public String getDockerImageName() {
    return dockerImageName;
  }

  public void setDockerImageName(String dockerImageName) {
    this.dockerImageName = dockerImageName;
  }

  /**
   * Getter for property 'loadBalancerName'.
   *
   * @return Value for property 'loadBalancerName'.
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Setter for property 'loadBalancerName'.
   *
   * @param loadBalancerName Value to set for property 'loadBalancerName'.
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'targetGroupArn'.
   *
   * @return Value for property 'targetGroupArn'.
   */
  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  /**
   * Setter for property 'targetGroupArn'.
   *
   * @param targetGroupArn Value to set for property 'targetGroupArn'.
   */
  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  /**
   * Getter for property 'roleArn'.
   *
   * @return Value for property 'roleArn'.
   */
  public String getRoleArn() {
    return roleArn;
  }

  /**
   * Setter for property 'roleArn'.
   *
   * @param roleArn Value to set for property 'roleArn'.
   */
  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "ecsClusterName",
        anExecutionDataValue().withValue(ecsClusterName).withDisplayName("ECS Cluster Name").build());
    putNotNull(executionDetails, "ecsServiceName",
        anExecutionDataValue().withValue(ecsServiceName).withDisplayName("ECS Service Name").build());
    putNotNull(executionDetails, "ecsOldServiceName",
        anExecutionDataValue().withValue(ecsOldServiceName).withDisplayName("ECS Old Service Name").build());
    putNotNull(executionDetails, "dockerImageName",
        anExecutionDataValue().withValue(dockerImageName).withDisplayName("Docker Image Name").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    putNotNull(executionDetails, "loadBalancerName",
        anExecutionDataValue().withValue(loadBalancerName).withDisplayName("Load Balancer").build());
    putNotNull(executionDetails, "targetGroupArn",
        anExecutionDataValue().withValue(emptyToNull(getTargetGroupName())).withDisplayName("Target Group").build());
    putNotNull(executionDetails, "roleArn",
        anExecutionDataValue()
            .withValue(emptyToNull(strip(substringAfterLast(roleArn, "/"))))
            .withDisplayName("ECS Role")
            .build());

    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "ecsClusterName",
        anExecutionDataValue().withValue(ecsClusterName).withDisplayName("ECS Cluster Name").build());
    putNotNull(executionDetails, "ecsServiceName",
        anExecutionDataValue().withValue(ecsServiceName).withDisplayName("ECS Service Name").build());
    putNotNull(executionDetails, "ecsOldServiceName",
        anExecutionDataValue().withValue(ecsOldServiceName).withDisplayName("ECS Old Service Name").build());
    putNotNull(executionDetails, "dockerImageName",
        anExecutionDataValue().withValue(dockerImageName).withDisplayName("Docker Image Name").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    putNotNull(executionDetails, "loadBalancerName",
        anExecutionDataValue().withValue(loadBalancerName).withDisplayName("Load Balancer").build());
    putNotNull(executionDetails, "targetGroupArn",
        anExecutionDataValue().withValue(emptyToNull(getTargetGroupName())).withDisplayName("Target Group").build());
    putNotNull(executionDetails, "roleArn",
        anExecutionDataValue()
            .withValue(emptyToNull(strip(substringAfterLast(roleArn, "/"))))
            .withDisplayName("ECS Role")
            .build());

    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    EcsStepExecutionSummary ecsStepExecutionSummary = new EcsStepExecutionSummary();
    populateStepExecutionSummary(ecsStepExecutionSummary);
    ecsStepExecutionSummary.setEcsServiceName(ecsServiceName);
    ecsStepExecutionSummary.setEcsOldServiceName(ecsOldServiceName);
    ecsStepExecutionSummary.setInstanceCount(instanceCount);
    return ecsStepExecutionSummary;
  }

  private String getTargetGroupName() {
    String targetGroupName = null;
    String[] targetGroupArnParts = split(targetGroupArn, "/");
    if (getLength(targetGroupArnParts) >= 2) {
      targetGroupName = targetGroupArnParts[1];
    }
    return targetGroupName;
  }

  public static final class Builder {
    private String ecsClusterName;
    private String ecsOldServiceName;
    private String ecsServiceName;
    private String stateName;
    private String dockerImageName;
    private Long startTs;
    private String commandName;
    private Long endTs;
    private int instanceCount;
    private ExecutionStatus status;
    private String loadBalancerName;
    private String errorMsg;
    private String targetGroupArn;
    private String roleArn;

    private Builder() {}

    public static Builder anEcsServiceExecutionData() {
      return new Builder();
    }

    public Builder withEcsClusterName(String ecsClusterName) {
      this.ecsClusterName = ecsClusterName;
      return this;
    }

    public Builder withEcsServiceName(String ecsServiceName) {
      this.ecsServiceName = ecsServiceName;
      return this;
    }

    public Builder withEcsOldServiceName(String ecsOldServiceName) {
      this.ecsOldServiceName = ecsOldServiceName;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withDockerImageName(String dockerImageName) {
      this.dockerImageName = dockerImageName;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withTargetGroupArn(String targetGroupArn) {
      this.targetGroupArn = targetGroupArn;
      return this;
    }

    public Builder withRoleArn(String roleArn) {
      this.roleArn = roleArn;
      return this;
    }

    public Builder but() {
      return anEcsServiceExecutionData()
          .withEcsClusterName(ecsClusterName)
          .withEcsServiceName(ecsServiceName)
          .withStateName(stateName)
          .withEcsOldServiceName(ecsOldServiceName)
          .withDockerImageName(dockerImageName)
          .withStartTs(startTs)
          .withCommandName(commandName)
          .withEndTs(endTs)
          .withInstanceCount(instanceCount)
          .withStatus(status)
          .withLoadBalancerName(loadBalancerName)
          .withErrorMsg(errorMsg)
          .withTargetGroupArn(targetGroupArn)
          .withRoleArn(roleArn);
    }

    public EcsServiceExecutionData build() {
      EcsServiceExecutionData ecsServiceExecutionData = new EcsServiceExecutionData();
      ecsServiceExecutionData.setEcsClusterName(ecsClusterName);
      ecsServiceExecutionData.setEcsServiceName(ecsServiceName);
      ecsServiceExecutionData.setEcsOldServiceName(ecsOldServiceName);
      ecsServiceExecutionData.setStateName(stateName);
      ecsServiceExecutionData.setDockerImageName(dockerImageName);
      ecsServiceExecutionData.setStartTs(startTs);
      ecsServiceExecutionData.setCommandName(commandName);
      ecsServiceExecutionData.setEndTs(endTs);
      ecsServiceExecutionData.setInstanceCount(instanceCount);
      ecsServiceExecutionData.setStatus(status);
      ecsServiceExecutionData.setLoadBalancerName(loadBalancerName);
      ecsServiceExecutionData.setErrorMsg(errorMsg);
      ecsServiceExecutionData.setTargetGroupArn(targetGroupArn);
      ecsServiceExecutionData.setRoleArn(roleArn);
      return ecsServiceExecutionData;
    }
  }
}
