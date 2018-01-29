package software.wings.beans.infrastructure.instance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;

/**
 * Represents the instance that the service get deployed onto.
 * We enforce unique constraint in code based on the instance key sub class.
 * @author rktummala
 */
@Entity(value = "instance", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class Instance extends Base {
  @Indexed @NotEmpty private InstanceType instanceType;
  @Indexed @Embedded private HostInstanceKey hostInstanceKey;
  @Indexed @Embedded private ContainerInstanceKey containerInstanceKey;
  @Indexed private String envId;
  private String envName;
  private EnvironmentType envType;
  @Indexed private String accountId;
  @Indexed private String serviceId;
  private String serviceName;
  private String appName;

  @Indexed private String infraMappingId;
  @Indexed private String infraMappingType;

  private String computeProviderId;
  private String computeProviderName;

  private String lastArtifactStreamId;

  @Indexed private String lastArtifactId;
  private String lastArtifactName;
  @Indexed private String lastArtifactSourceName;
  @Indexed private String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;
  @Indexed private String lastWorkflowExecutionId;
  private String lastWorkflowExecutionName;

  @Indexed private String lastPipelineExecutionId;
  private String lastPipelineExecutionName;

  @Embedded private InstanceInfo instanceInfo;

  public static final class Builder {
    protected String appId;
    private InstanceType instanceType;
    private HostInstanceKey hostInstanceKey;
    private ContainerInstanceKey containerInstanceKey;
    private String envId;
    private String envName;
    private EnvironmentType envType;
    private String accountId;
    private String serviceId;
    private String serviceName;
    private String appName;
    private String uuid;
    private String infraMappingId;
    private String infraMappingType;
    private String computeProviderId;
    private EmbeddedUser createdBy;
    private String computeProviderName;
    private long createdAt;
    private String lastArtifactStreamId;
    private EmbeddedUser lastUpdatedBy;
    private String lastArtifactId;
    private String lastArtifactName;
    private long lastUpdatedAt;
    private String lastArtifactSourceName;
    private String lastArtifactBuildNum;
    private String lastDeployedById;
    private String lastDeployedByName;
    private long lastDeployedAt;
    private String lastWorkflowExecutionId;
    private String lastWorkflowExecutionName;
    private String lastPipelineExecutionId;
    private String lastPipelineExecutionName;
    private InstanceInfo instanceInfo;

    private Builder() {}

    public static Builder anInstance() {
      return new Builder();
    }

    public Builder withInstanceType(InstanceType instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    public Builder withHostInstanceKey(HostInstanceKey hostInstanceKey) {
      this.hostInstanceKey = hostInstanceKey;
      return this;
    }

    public Builder withContainerInstanceKey(ContainerInstanceKey containerInstanceKey) {
      this.containerInstanceKey = containerInstanceKey;
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

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
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

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastArtifactStreamId(String lastArtifactStreamId) {
      this.lastArtifactStreamId = lastArtifactStreamId;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastArtifactId(String lastArtifactId) {
      this.lastArtifactId = lastArtifactId;
      return this;
    }

    public Builder withLastArtifactName(String lastArtifactName) {
      this.lastArtifactName = lastArtifactName;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withLastArtifactSourceName(String lastArtifactSourceName) {
      this.lastArtifactSourceName = lastArtifactSourceName;
      return this;
    }

    public Builder withLastArtifactBuildNum(String lastArtifactBuildNum) {
      this.lastArtifactBuildNum = lastArtifactBuildNum;
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

    public Builder withLastDeployedAt(long lastDeployedAt) {
      this.lastDeployedAt = lastDeployedAt;
      return this;
    }

    public Builder withLastWorkflowExecutionId(String lastWorkflowExecutionId) {
      this.lastWorkflowExecutionId = lastWorkflowExecutionId;
      return this;
    }

    public Builder withLastWorkflowExecutionName(String lastWorkflowExecutionName) {
      this.lastWorkflowExecutionName = lastWorkflowExecutionName;
      return this;
    }

    public Builder withLastPipelineExecutionId(String lastPipelineExecutionId) {
      this.lastPipelineExecutionId = lastPipelineExecutionId;
      return this;
    }

    public Builder withLastPipelineExecutionName(String lastPipelineExecutionName) {
      this.lastPipelineExecutionName = lastPipelineExecutionName;
      return this;
    }

    public Builder withInstanceInfo(InstanceInfo instanceInfo) {
      this.instanceInfo = instanceInfo;
      return this;
    }

    public Builder but() {
      return anInstance()
          .withInstanceType(instanceType)
          .withHostInstanceKey(hostInstanceKey)
          .withContainerInstanceKey(containerInstanceKey)
          .withEnvId(envId)
          .withEnvName(envName)
          .withEnvType(envType)
          .withAccountId(accountId)
          .withServiceId(serviceId)
          .withServiceName(serviceName)
          .withAppName(appName)
          .withUuid(uuid)
          .withInfraMappingId(infraMappingId)
          .withInfraMappingType(infraMappingType)
          .withAppId(appId)
          .withComputeProviderId(computeProviderId)
          .withCreatedBy(createdBy)
          .withComputeProviderName(computeProviderName)
          .withCreatedAt(createdAt)
          .withLastArtifactStreamId(lastArtifactStreamId)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastArtifactId(lastArtifactId)
          .withLastArtifactName(lastArtifactName)
          .withLastUpdatedAt(lastUpdatedAt)
          .withLastArtifactSourceName(lastArtifactSourceName)
          .withLastArtifactBuildNum(lastArtifactBuildNum)
          .withLastDeployedById(lastDeployedById)
          .withLastDeployedByName(lastDeployedByName)
          .withLastDeployedAt(lastDeployedAt)
          .withLastWorkflowExecutionId(lastWorkflowExecutionId)
          .withLastWorkflowExecutionName(lastWorkflowExecutionName)
          .withLastPipelineExecutionId(lastPipelineExecutionId)
          .withLastPipelineExecutionName(lastPipelineExecutionName)
          .withInstanceInfo(instanceInfo);
    }

    public Instance build() {
      Instance instance = new Instance();
      instance.setInstanceType(instanceType);
      instance.setHostInstanceKey(hostInstanceKey);
      instance.setContainerInstanceKey(containerInstanceKey);
      instance.setEnvId(envId);
      instance.setEnvName(envName);
      instance.setEnvType(envType);
      instance.setAccountId(accountId);
      instance.setServiceId(serviceId);
      instance.setServiceName(serviceName);
      instance.setAppName(appName);
      instance.setUuid(uuid);
      instance.setInfraMappingId(infraMappingId);
      instance.setInfraMappingType(infraMappingType);
      instance.setAppId(appId);
      instance.setComputeProviderId(computeProviderId);
      instance.setCreatedBy(createdBy);
      instance.setComputeProviderName(computeProviderName);
      instance.setCreatedAt(createdAt);
      instance.setLastArtifactStreamId(lastArtifactStreamId);
      instance.setLastUpdatedBy(lastUpdatedBy);
      instance.setLastArtifactId(lastArtifactId);
      instance.setLastArtifactName(lastArtifactName);
      instance.setLastUpdatedAt(lastUpdatedAt);
      instance.setLastArtifactSourceName(lastArtifactSourceName);
      instance.setLastArtifactBuildNum(lastArtifactBuildNum);
      instance.setLastDeployedById(lastDeployedById);
      instance.setLastDeployedByName(lastDeployedByName);
      instance.setLastDeployedAt(lastDeployedAt);
      instance.setLastWorkflowExecutionId(lastWorkflowExecutionId);
      instance.setLastWorkflowExecutionName(lastWorkflowExecutionName);
      instance.setLastPipelineExecutionId(lastPipelineExecutionId);
      instance.setLastPipelineExecutionName(lastPipelineExecutionName);
      instance.setInstanceInfo(instanceInfo);
      return instance;
    }
  }
}
