package software.wings.beans.infrastructure.instance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;

/**
 * @author rktummala on 09/07/17
 */
@Data
@EqualsAndHashCode
public class KubernetesContainerDeploymentInfo extends ContainerDeploymentInfo {
  private List<String> replicationControllerNameList;

  public String getRcNameWithoutRevision(String replicationControllerName) {
    if (replicationControllerName == null) {
      return null;
    }

    // replicationControllerName looks like abaris.dockerfromgke.development.22
    // we need to extract abaris.dockerfromgke.development so that we can pull all the instances of all the revisions at
    // once.
    int index = replicationControllerName.lastIndexOf(".");
    if (index == -1) {
      return replicationControllerName;
    }
    return replicationControllerName.substring(0, index);
  }

  public static final class Builder {
    protected String appId;
    protected String lastArtifactBuildNum;
    private List<String> replicationControllerNameList;
    private String clusterName;
    private String envId;
    private String envName;
    private EnvironmentType envType;
    private String accountId;
    private String serviceId;
    private String serviceName;
    private String appName;
    private String infraMappingId;
    private String infraMappingType;
    private String computeProviderId;
    private String computeProviderName;
    private String lastArtifactStreamId;
    private String lastArtifactId;
    private String uuid;
    private String lastArtifactName;
    private String lastArtifactSourceName;
    private EmbeddedUser createdBy;
    private String lastDeployedById;
    private String lastDeployedByName;
    private long createdAt;
    private long lastDeployedAt;
    private EmbeddedUser lastUpdatedBy;
    private String lastWorkflowId;
    private long lastUpdatedAt;
    private String lastWorkflowName;
    private String lastPipelineId;
    private String lastPipelineName;

    private Builder() {}

    public static Builder aKubernetesContainerDeploymentInfo() {
      return new Builder();
    }

    public Builder withReplicationControllerNameList(List<String> replicationControllerNameList) {
      this.replicationControllerNameList = replicationControllerNameList;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public Builder withEnvType(EnvironmentType envType) {
      this.envType = envType;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
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

    public Builder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public Builder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withLastArtifactStreamId(String lastArtifactStreamId) {
      this.lastArtifactStreamId = lastArtifactStreamId;
      return this;
    }

    public Builder withLastArtifactId(String lastArtifactId) {
      this.lastArtifactId = lastArtifactId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withLastArtifactName(String lastArtifactName) {
      this.lastArtifactName = lastArtifactName;
      return this;
    }

    public Builder withLastArtifactSourceName(String lastArtifactSourceName) {
      this.lastArtifactSourceName = lastArtifactSourceName;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withLastArtifactBuildNum(String lastArtifactBuildNum) {
      this.lastArtifactBuildNum = lastArtifactBuildNum;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withLastDeployedById(String lastDeployedById) {
      this.lastDeployedById = lastDeployedById;
      return this;
    }

    public Builder withLastDeployedByName(String lastDeployedByName) {
      this.lastDeployedByName = lastDeployedByName;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastDeployedAt(long lastDeployedAt) {
      this.lastDeployedAt = lastDeployedAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastWorkflowId(String lastWorkflowId) {
      this.lastWorkflowId = lastWorkflowId;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withLastWorkflowName(String lastWorkflowName) {
      this.lastWorkflowName = lastWorkflowName;
      return this;
    }

    public Builder withLastPipelineId(String lastPipelineId) {
      this.lastPipelineId = lastPipelineId;
      return this;
    }

    public Builder withLastPipelineName(String lastPipelineName) {
      this.lastPipelineName = lastPipelineName;
      return this;
    }

    public Builder but() {
      return aKubernetesContainerDeploymentInfo()
          .withReplicationControllerNameList(replicationControllerNameList)
          .withClusterName(clusterName)
          .withEnvId(envId)
          .withEnvName(envName)
          .withEnvType(envType)
          .withAccountId(accountId)
          .withServiceId(serviceId)
          .withServiceName(serviceName)
          .withAppName(appName)
          .withInfraMappingId(infraMappingId)
          .withInfraMappingType(infraMappingType)
          .withComputeProviderId(computeProviderId)
          .withComputeProviderName(computeProviderName)
          .withLastArtifactStreamId(lastArtifactStreamId)
          .withLastArtifactId(lastArtifactId)
          .withUuid(uuid)
          .withLastArtifactName(lastArtifactName)
          .withLastArtifactSourceName(lastArtifactSourceName)
          .withAppId(appId)
          .withLastArtifactBuildNum(lastArtifactBuildNum)
          .withCreatedBy(createdBy)
          .withLastDeployedById(lastDeployedById)
          .withLastDeployedByName(lastDeployedByName)
          .withCreatedAt(createdAt)
          .withLastDeployedAt(lastDeployedAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastWorkflowId(lastWorkflowId)
          .withLastUpdatedAt(lastUpdatedAt)
          .withLastWorkflowName(lastWorkflowName)
          .withLastPipelineId(lastPipelineId)
          .withLastPipelineName(lastPipelineName);
    }

    public KubernetesContainerDeploymentInfo build() {
      KubernetesContainerDeploymentInfo kubernetesContainerDeploymentInfo = new KubernetesContainerDeploymentInfo();
      kubernetesContainerDeploymentInfo.setReplicationControllerNameList(replicationControllerNameList);
      kubernetesContainerDeploymentInfo.setClusterName(clusterName);
      kubernetesContainerDeploymentInfo.setEnvId(envId);
      kubernetesContainerDeploymentInfo.setEnvName(envName);
      kubernetesContainerDeploymentInfo.setEnvType(envType);
      kubernetesContainerDeploymentInfo.setAccountId(accountId);
      kubernetesContainerDeploymentInfo.setServiceId(serviceId);
      kubernetesContainerDeploymentInfo.setServiceName(serviceName);
      kubernetesContainerDeploymentInfo.setAppName(appName);
      kubernetesContainerDeploymentInfo.setInfraMappingId(infraMappingId);
      kubernetesContainerDeploymentInfo.setInfraMappingType(infraMappingType);
      kubernetesContainerDeploymentInfo.setComputeProviderId(computeProviderId);
      kubernetesContainerDeploymentInfo.setComputeProviderName(computeProviderName);
      kubernetesContainerDeploymentInfo.setLastArtifactStreamId(lastArtifactStreamId);
      kubernetesContainerDeploymentInfo.setLastArtifactId(lastArtifactId);
      kubernetesContainerDeploymentInfo.setUuid(uuid);
      kubernetesContainerDeploymentInfo.setLastArtifactName(lastArtifactName);
      kubernetesContainerDeploymentInfo.setLastArtifactSourceName(lastArtifactSourceName);
      kubernetesContainerDeploymentInfo.setAppId(appId);
      kubernetesContainerDeploymentInfo.setLastArtifactBuildNum(lastArtifactBuildNum);
      kubernetesContainerDeploymentInfo.setCreatedBy(createdBy);
      kubernetesContainerDeploymentInfo.setLastDeployedById(lastDeployedById);
      kubernetesContainerDeploymentInfo.setLastDeployedByName(lastDeployedByName);
      kubernetesContainerDeploymentInfo.setCreatedAt(createdAt);
      kubernetesContainerDeploymentInfo.setLastDeployedAt(lastDeployedAt);
      kubernetesContainerDeploymentInfo.setLastUpdatedBy(lastUpdatedBy);
      kubernetesContainerDeploymentInfo.setLastWorkflowId(lastWorkflowId);
      kubernetesContainerDeploymentInfo.setLastUpdatedAt(lastUpdatedAt);
      kubernetesContainerDeploymentInfo.setLastWorkflowName(lastWorkflowName);
      kubernetesContainerDeploymentInfo.setLastPipelineId(lastPipelineId);
      kubernetesContainerDeploymentInfo.setLastPipelineName(lastPipelineName);
      return kubernetesContainerDeploymentInfo;
    }
  }
}
