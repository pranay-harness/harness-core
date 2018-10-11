package software.wings.api;

import static com.google.common.base.Strings.emptyToNull;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.task.protocol.ResponseData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;

/**
 * Created by rishi on 2/15/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class EcsServiceExecutionData extends StateExecutionData implements ResponseData {
  private String ecsClusterName;
  private String ecsServiceName;
  private String dockerImageName;
  private String commandName;
  private int instanceCount;
  private String loadBalancerName;
  private String targetGroupArn;
  private String roleArn;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "ecsClusterName",
        ExecutionDataValue.builder().displayName("ECS Cluster Name").value(ecsClusterName).build());
    putNotNull(executionDetails, "ecsServiceName",
        ExecutionDataValue.builder().displayName("ECS Service Name").value(ecsServiceName).build());
    putNotNull(executionDetails, "dockerImageName",
        ExecutionDataValue.builder().displayName("Docker Image Name").value(dockerImageName).build());
    putNotNull(executionDetails, "commandName",
        ExecutionDataValue.builder().displayName("Command Name").value(commandName).build());
    putNotNull(executionDetails, "loadBalancerName",
        ExecutionDataValue.builder().displayName("Load Balancer").value(loadBalancerName).build());
    putNotNull(executionDetails, "targetGroupArn",
        ExecutionDataValue.builder().displayName("Target Group").value(emptyToNull(getTargetGroupName())).build());
    putNotNull(executionDetails, "roleArn",
        ExecutionDataValue.builder()
            .displayName("ECS Role")
            .value(emptyToNull(strip(substringAfterLast(roleArn, "/"))))
            .build());

    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "ecsClusterName",
        ExecutionDataValue.builder().displayName("ECS Cluster Name").value(ecsClusterName).build());
    putNotNull(executionDetails, "ecsServiceName",
        ExecutionDataValue.builder().displayName("ECS Service Name").value(ecsServiceName).build());
    putNotNull(executionDetails, "dockerImageName",
        ExecutionDataValue.builder().displayName("Docker Image Name").value(dockerImageName).build());
    putNotNull(executionDetails, "commandName",
        ExecutionDataValue.builder().displayName("Command Name").value(commandName).build());
    putNotNull(executionDetails, "loadBalancerName",
        ExecutionDataValue.builder().displayName("Load Balancer").value(loadBalancerName).build());
    putNotNull(executionDetails, "targetGroupArn",
        ExecutionDataValue.builder().displayName("Target Group").value(emptyToNull(getTargetGroupName())).build());
    putNotNull(executionDetails, "roleArn",
        ExecutionDataValue.builder()
            .displayName("ECS Role")
            .value(emptyToNull(strip(substringAfterLast(roleArn, "/"))))
            .build());

    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    EcsStepExecutionSummary ecsStepExecutionSummary = new EcsStepExecutionSummary();
    populateStepExecutionSummary(ecsStepExecutionSummary);
    ecsStepExecutionSummary.setEcsServiceName(ecsServiceName);
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
