package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rishi on 2/15/17.
 */
public class EcsServiceExecutionData extends StateExecutionData implements NotifyResponseData {
  private String ecsClusterName;
  private String ecsServiceName;
  private String dockerImageName;
  private String commandName;
  private int instanceCount;

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
  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "ecsClusterName",
        anExecutionDataValue().withValue(ecsClusterName).withDisplayName("ECS Cluster Name").build());
    putNotNull(executionDetails, "ecsServiceName",
        anExecutionDataValue().withValue(ecsServiceName).withDisplayName("ECS Service Name").build());
    putNotNull(executionDetails, "dockerImageName",
        anExecutionDataValue().withValue(dockerImageName).withDisplayName("Docker Image Name").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    putNotNull(executionDetails, "instanceCount",
        anExecutionDataValue().withValue(instanceCount).withDisplayName("Instance Count").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "ecsClusterName",
        anExecutionDataValue().withValue(ecsClusterName).withDisplayName("ECS Cluster Name").build());
    putNotNull(executionDetails, "ecsServiceName",
        anExecutionDataValue().withValue(ecsServiceName).withDisplayName("ECS Service Name").build());
    putNotNull(executionDetails, "dockerImageName",
        anExecutionDataValue().withValue(dockerImageName).withDisplayName("Docker Image Name").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    putNotNull(executionDetails, "instanceCount",
        anExecutionDataValue().withValue(instanceCount).withDisplayName("Instance Count").build());
    return executionDetails;
  }

  public static final class EcsServiceExecutionDataBuilder {
    private String ecsClusterName;
    private String ecsServiceName;
    private String dockerImageName;
    private String commandName;
    private int instanceCount;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private EcsServiceExecutionDataBuilder() {}

    public static EcsServiceExecutionDataBuilder anEcsServiceExecutionData() {
      return new EcsServiceExecutionDataBuilder();
    }

    public EcsServiceExecutionDataBuilder withEcsClusterName(String ecsClusterName) {
      this.ecsClusterName = ecsClusterName;
      return this;
    }

    public EcsServiceExecutionDataBuilder withEcsServiceName(String ecsServiceName) {
      this.ecsServiceName = ecsServiceName;
      return this;
    }

    public EcsServiceExecutionDataBuilder withDockerImageName(String dockerImageName) {
      this.dockerImageName = dockerImageName;
      return this;
    }

    public EcsServiceExecutionDataBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public EcsServiceExecutionDataBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsServiceExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public EcsServiceExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public EcsServiceExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public EcsServiceExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public EcsServiceExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public EcsServiceExecutionData build() {
      EcsServiceExecutionData ecsServiceExecutionData = new EcsServiceExecutionData();
      ecsServiceExecutionData.setEcsClusterName(ecsClusterName);
      ecsServiceExecutionData.setEcsServiceName(ecsServiceName);
      ecsServiceExecutionData.setDockerImageName(dockerImageName);
      ecsServiceExecutionData.setCommandName(commandName);
      ecsServiceExecutionData.setInstanceCount(instanceCount);
      ecsServiceExecutionData.setStateName(stateName);
      ecsServiceExecutionData.setStartTs(startTs);
      ecsServiceExecutionData.setEndTs(endTs);
      ecsServiceExecutionData.setStatus(status);
      ecsServiceExecutionData.setErrorMsg(errorMsg);
      return ecsServiceExecutionData;
    }
  }
}
