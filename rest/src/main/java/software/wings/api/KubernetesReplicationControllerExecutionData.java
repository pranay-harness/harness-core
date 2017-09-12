package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by brett on 3/13/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesReplicationControllerExecutionData extends StateExecutionData implements NotifyResponseData {
  private String gkeClusterName;
  private String kubernetesReplicationControllerName;
  private String kubernetesServiceName;
  private String kubernetesServiceClusterIP;
  private String kubernetesServiceLoadBalancerEndpoint;
  private String dockerImageName;
  private String commandName;
  private int instanceCount;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "gkeClusterName",
        anExecutionDataValue().withValue(gkeClusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "kubernetesReplicationControllerName",
        anExecutionDataValue()
            .withValue(kubernetesReplicationControllerName)
            .withDisplayName("Replication Controller Name")
            .build());
    putNotNull(executionDetails, "kubernetesServiceName",
        anExecutionDataValue().withValue(kubernetesServiceName).withDisplayName("Service Name").build());
    putNotNull(executionDetails, "kubernetesServiceClusterIP",
        anExecutionDataValue().withValue(kubernetesServiceClusterIP).withDisplayName("Service Cluster IP").build());
    putNotNull(executionDetails, "kubernetesServiceLoadBalancerEndpoint",
        anExecutionDataValue()
            .withValue(kubernetesServiceLoadBalancerEndpoint)
            .withDisplayName("Load Balancer Endpoint")
            .build());
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
    putNotNull(executionDetails, "gkeClusterName",
        anExecutionDataValue().withValue(gkeClusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "kubernetesReplicationControllerName",
        anExecutionDataValue()
            .withValue(kubernetesReplicationControllerName)
            .withDisplayName("Replication Controller Name")
            .build());
    putNotNull(executionDetails, "kubernetesServiceName",
        anExecutionDataValue().withValue(kubernetesServiceName).withDisplayName("Service Name").build());
    putNotNull(executionDetails, "kubernetesServiceClusterIP",
        anExecutionDataValue().withValue(kubernetesServiceClusterIP).withDisplayName("Service Cluster IP").build());
    putNotNull(executionDetails, "kubernetesServiceLoadBalancerEndpoint",
        anExecutionDataValue()
            .withValue(kubernetesServiceLoadBalancerEndpoint)
            .withDisplayName("Load Balancer Endpoint")
            .build());
    putNotNull(executionDetails, "dockerImageName",
        anExecutionDataValue().withValue(dockerImageName).withDisplayName("Docker Image Name").build());
    putNotNull(executionDetails, "commandName",
        anExecutionDataValue().withValue(commandName).withDisplayName("Command Name").build());
    putNotNull(executionDetails, "instanceCount",
        anExecutionDataValue().withValue(instanceCount).withDisplayName("Instance Count").build());
    return executionDetails;
  }

  public static final class KubernetesReplicationControllerExecutionDataBuilder {
    private String gkeClusterName;
    private String kubernetesReplicationControllerName;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private String kubernetesServiceName;
    private ExecutionStatus status;
    private String kubernetesServiceClusterIP;
    private String errorMsg;
    private String kubernetesServiceLoadBalancerEndpoint;
    private String dockerImageName;
    private String commandName;
    private int instanceCount;

    private KubernetesReplicationControllerExecutionDataBuilder() {}

    public static KubernetesReplicationControllerExecutionDataBuilder aKubernetesReplicationControllerExecutionData() {
      return new KubernetesReplicationControllerExecutionDataBuilder();
    }

    public KubernetesReplicationControllerExecutionDataBuilder withGkeClusterName(String gkeClusterName) {
      this.gkeClusterName = gkeClusterName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withKubernetesReplicationControllerName(
        String kubernetesReplicationControllerName) {
      this.kubernetesReplicationControllerName = kubernetesReplicationControllerName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withKubernetesServiceName(String kubernetesServiceName) {
      this.kubernetesServiceName = kubernetesServiceName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withKubernetesServiceClusterIP(
        String kubernetesServiceClusterIP) {
      this.kubernetesServiceClusterIP = kubernetesServiceClusterIP;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withKubernetesServiceLoadBalancerEndpoint(
        String kubernetesServiceLoadBalancerEndpoint) {
      this.kubernetesServiceLoadBalancerEndpoint = kubernetesServiceLoadBalancerEndpoint;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withDockerImageName(String dockerImageName) {
      this.dockerImageName = dockerImageName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesReplicationControllerExecutionDataBuilder but() {
      return aKubernetesReplicationControllerExecutionData()
          .withGkeClusterName(gkeClusterName)
          .withKubernetesReplicationControllerName(kubernetesReplicationControllerName)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withKubernetesServiceName(kubernetesServiceName)
          .withStatus(status)
          .withKubernetesServiceClusterIP(kubernetesServiceClusterIP)
          .withErrorMsg(errorMsg)
          .withKubernetesServiceLoadBalancerEndpoint(kubernetesServiceLoadBalancerEndpoint)
          .withDockerImageName(dockerImageName)
          .withCommandName(commandName)
          .withInstanceCount(instanceCount);
    }

    public KubernetesReplicationControllerExecutionData build() {
      KubernetesReplicationControllerExecutionData kubernetesReplicationControllerExecutionData =
          new KubernetesReplicationControllerExecutionData();
      kubernetesReplicationControllerExecutionData.setGkeClusterName(gkeClusterName);
      kubernetesReplicationControllerExecutionData.setKubernetesReplicationControllerName(
          kubernetesReplicationControllerName);
      kubernetesReplicationControllerExecutionData.setStateName(stateName);
      kubernetesReplicationControllerExecutionData.setStartTs(startTs);
      kubernetesReplicationControllerExecutionData.setEndTs(endTs);
      kubernetesReplicationControllerExecutionData.setKubernetesServiceName(kubernetesServiceName);
      kubernetesReplicationControllerExecutionData.setStatus(status);
      kubernetesReplicationControllerExecutionData.setKubernetesServiceClusterIP(kubernetesServiceClusterIP);
      kubernetesReplicationControllerExecutionData.setErrorMsg(errorMsg);
      kubernetesReplicationControllerExecutionData.setKubernetesServiceLoadBalancerEndpoint(
          kubernetesServiceLoadBalancerEndpoint);
      kubernetesReplicationControllerExecutionData.setDockerImageName(dockerImageName);
      kubernetesReplicationControllerExecutionData.setCommandName(commandName);
      kubernetesReplicationControllerExecutionData.setInstanceCount(instanceCount);
      return kubernetesReplicationControllerExecutionData;
    }
  }
}
