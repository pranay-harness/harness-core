package software.wings.beans.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment.EnvironmentType;

import java.util.Objects;

/**
 * Represents the instance that the service get deployed onto.
 */
@Entity(value = "instance", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("hostName")
                           , @Field("infraMappingId") }, options = @IndexOptions(unique = true)))
public class Instance extends Base {
  @NotEmpty private String envId;
  @NotEmpty private String envName;
  @NotEmpty private EnvironmentType envType;

  @NotEmpty private String accountId;
  @Indexed @NotEmpty private String serviceId;
  @NotEmpty private String serviceName;
  @Indexed @NotEmpty private String appName;

  @NotEmpty private String hostId;
  @Indexed @NotEmpty private String hostName;
  @NotEmpty private String hostPublicDns;

  @Indexed @NotEmpty private String infraMappingId;
  @NotEmpty private String infraMappingType;

  private String computeProviderId;
  private String computeProviderName;

  @NotEmpty private String lastArtifactStreamId;

  @Indexed @NotEmpty private String lastArtifactId;
  @NotEmpty private String lastArtifactName;
  @Indexed @NotEmpty private String lastArtifactSourceName;
  @NotEmpty private String lastArtifactBuildNum;

  @NotEmpty private String lastDeployedById;
  @NotEmpty private String lastDeployedByName;
  @NotEmpty private long lastDeployedAt;

  @NotEmpty private String lastWorkflowId;
  @NotEmpty private String lastWorkflowName;

  private String lastPipelineId;
  private String lastPipelineName;

  @Embedded private InstanceMetadata metadata;

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public EnvironmentType getEnvType() {
    return envType;
  }

  public void setEnvType(EnvironmentType envType) {
    this.envType = envType;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getHostPublicDns() {
    return hostPublicDns;
  }

  public void setHostPublicDns(String hostPublicDns) {
    this.hostPublicDns = hostPublicDns;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public String getInfraMappingType() {
    return infraMappingType;
  }

  public void setInfraMappingType(String infraMappingType) {
    this.infraMappingType = infraMappingType;
  }

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  public String getComputeProviderName() {
    return computeProviderName;
  }

  public void setComputeProviderName(String computeProviderName) {
    this.computeProviderName = computeProviderName;
  }

  public String getLastArtifactStreamId() {
    return lastArtifactStreamId;
  }

  public void setLastArtifactStreamId(String lastArtifactStreamId) {
    this.lastArtifactStreamId = lastArtifactStreamId;
  }

  public String getLastArtifactId() {
    return lastArtifactId;
  }

  public void setLastArtifactId(String lastArtifactId) {
    this.lastArtifactId = lastArtifactId;
  }

  public String getLastArtifactName() {
    return lastArtifactName;
  }

  public void setLastArtifactName(String lastArtifactName) {
    this.lastArtifactName = lastArtifactName;
  }

  public String getLastArtifactSourceName() {
    return lastArtifactSourceName;
  }

  public void setLastArtifactSourceName(String lastArtifactSourceName) {
    this.lastArtifactSourceName = lastArtifactSourceName;
  }

  public String getLastArtifactBuildNum() {
    return lastArtifactBuildNum;
  }

  public void setLastArtifactBuildNum(String lastArtifactBuildNum) {
    this.lastArtifactBuildNum = lastArtifactBuildNum;
  }

  public String getLastDeployedById() {
    return lastDeployedById;
  }

  public void setLastDeployedById(String lastDeployedById) {
    this.lastDeployedById = lastDeployedById;
  }

  public String getLastDeployedByName() {
    return lastDeployedByName;
  }

  public void setLastDeployedByName(String lastDeployedByName) {
    this.lastDeployedByName = lastDeployedByName;
  }

  public long getLastDeployedAt() {
    return lastDeployedAt;
  }

  public void setLastDeployedAt(long lastDeployedAt) {
    this.lastDeployedAt = lastDeployedAt;
  }

  public String getLastWorkflowId() {
    return lastWorkflowId;
  }

  public void setLastWorkflowId(String lastWorkflowId) {
    this.lastWorkflowId = lastWorkflowId;
  }

  public String getLastWorkflowName() {
    return lastWorkflowName;
  }

  public void setLastWorkflowName(String lastWorkflowName) {
    this.lastWorkflowName = lastWorkflowName;
  }

  public String getLastPipelineId() {
    return lastPipelineId;
  }

  public void setLastPipelineId(String lastPipelineId) {
    this.lastPipelineId = lastPipelineId;
  }

  public String getLastPipelineName() {
    return lastPipelineName;
  }

  public void setLastPipelineName(String lastPipelineName) {
    this.lastPipelineName = lastPipelineName;
  }

  public InstanceMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(InstanceMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    Instance instance = (Instance) o;

    if (lastDeployedAt != instance.lastDeployedAt)
      return false;
    if (envId != null ? !envId.equals(instance.envId) : instance.envId != null)
      return false;
    if (envName != null ? !envName.equals(instance.envName) : instance.envName != null)
      return false;
    if (envType != instance.envType)
      return false;
    if (accountId != null ? !accountId.equals(instance.accountId) : instance.accountId != null)
      return false;
    if (serviceId != null ? !serviceId.equals(instance.serviceId) : instance.serviceId != null)
      return false;
    if (serviceName != null ? !serviceName.equals(instance.serviceName) : instance.serviceName != null)
      return false;
    if (appName != null ? !appName.equals(instance.appName) : instance.appName != null)
      return false;
    if (hostId != null ? !hostId.equals(instance.hostId) : instance.hostId != null)
      return false;
    if (hostName != null ? !hostName.equals(instance.hostName) : instance.hostName != null)
      return false;
    if (hostPublicDns != null ? !hostPublicDns.equals(instance.hostPublicDns) : instance.hostPublicDns != null)
      return false;
    if (infraMappingId != null ? !infraMappingId.equals(instance.infraMappingId) : instance.infraMappingId != null)
      return false;
    if (infraMappingType != null ? !infraMappingType.equals(instance.infraMappingType)
                                 : instance.infraMappingType != null)
      return false;
    if (computeProviderId != null ? !computeProviderId.equals(instance.computeProviderId)
                                  : instance.computeProviderId != null)
      return false;
    if (computeProviderName != null ? !computeProviderName.equals(instance.computeProviderName)
                                    : instance.computeProviderName != null)
      return false;
    if (lastArtifactStreamId != null ? !lastArtifactStreamId.equals(instance.lastArtifactStreamId)
                                     : instance.lastArtifactStreamId != null)
      return false;
    if (lastArtifactId != null ? !lastArtifactId.equals(instance.lastArtifactId) : instance.lastArtifactId != null)
      return false;
    if (lastArtifactName != null ? !lastArtifactName.equals(instance.lastArtifactName)
                                 : instance.lastArtifactName != null)
      return false;
    if (lastArtifactSourceName != null ? !lastArtifactSourceName.equals(instance.lastArtifactSourceName)
                                       : instance.lastArtifactSourceName != null)
      return false;
    if (lastArtifactBuildNum != null ? !lastArtifactBuildNum.equals(instance.lastArtifactBuildNum)
                                     : instance.lastArtifactBuildNum != null)
      return false;
    if (lastDeployedById != null ? !lastDeployedById.equals(instance.lastDeployedById)
                                 : instance.lastDeployedById != null)
      return false;
    if (lastDeployedByName != null ? !lastDeployedByName.equals(instance.lastDeployedByName)
                                   : instance.lastDeployedByName != null)
      return false;
    if (lastWorkflowId != null ? !lastWorkflowId.equals(instance.lastWorkflowId) : instance.lastWorkflowId != null)
      return false;
    if (lastWorkflowName != null ? !lastWorkflowName.equals(instance.lastWorkflowName)
                                 : instance.lastWorkflowName != null)
      return false;
    if (lastPipelineId != null ? !lastPipelineId.equals(instance.lastPipelineId) : instance.lastPipelineId != null)
      return false;
    if (lastPipelineName != null ? !lastPipelineName.equals(instance.lastPipelineName)
                                 : instance.lastPipelineName != null)
      return false;
    return metadata != null ? metadata.equals(instance.metadata) : instance.metadata == null;
  }

  @Override
  public String toString() {
    return "Instance{"
        + "envId='" + envId + '\'' + ", envName='" + envName + '\'' + ", envType=" + envType + ", accountId='"
        + accountId + '\'' + ", serviceId='" + serviceId + '\'' + ", serviceName='" + serviceName + '\'' + ", appName='"
        + appName + '\'' + ", hostId='" + hostId + '\'' + ", hostName='" + hostName + '\'' + ", hostPublicDns='"
        + hostPublicDns + '\'' + ", infraMappingId='" + infraMappingId + '\'' + ", infraMappingType='"
        + infraMappingType + '\'' + ", computeProviderId='" + computeProviderId + '\'' + ", computeProviderName='"
        + computeProviderName + '\'' + ", lastArtifactStreamId='" + lastArtifactStreamId + '\'' + ", lastArtifactId='"
        + lastArtifactId + '\'' + ", lastArtifactName='" + lastArtifactName + '\'' + ", lastArtifactSourceName='"
        + lastArtifactSourceName + '\'' + ", lastArtifactBuildNum='" + lastArtifactBuildNum + '\''
        + ", lastDeployedById='" + lastDeployedById + '\'' + ", lastDeployedByName='" + lastDeployedByName + '\''
        + ", lastDeployedAt=" + lastDeployedAt + ", lastWorkflowId='" + lastWorkflowId + '\'' + ", lastWorkflowName='"
        + lastWorkflowName + '\'' + ", lastPipelineId='" + lastPipelineId + '\'' + ", lastPipelineName='"
        + lastPipelineName + '\'' + ", metadata=" + metadata + '}';
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (envId != null ? envId.hashCode() : 0);
    result = 31 * result + (envName != null ? envName.hashCode() : 0);
    result = 31 * result + (envType != null ? envType.hashCode() : 0);
    result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (appName != null ? appName.hashCode() : 0);
    result = 31 * result + (hostId != null ? hostId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (hostPublicDns != null ? hostPublicDns.hashCode() : 0);
    result = 31 * result + (infraMappingId != null ? infraMappingId.hashCode() : 0);
    result = 31 * result + (infraMappingType != null ? infraMappingType.hashCode() : 0);
    result = 31 * result + (computeProviderId != null ? computeProviderId.hashCode() : 0);
    result = 31 * result + (computeProviderName != null ? computeProviderName.hashCode() : 0);
    result = 31 * result + (lastArtifactStreamId != null ? lastArtifactStreamId.hashCode() : 0);
    result = 31 * result + (lastArtifactId != null ? lastArtifactId.hashCode() : 0);
    result = 31 * result + (lastArtifactName != null ? lastArtifactName.hashCode() : 0);
    result = 31 * result + (lastArtifactSourceName != null ? lastArtifactSourceName.hashCode() : 0);
    result = 31 * result + (lastArtifactBuildNum != null ? lastArtifactBuildNum.hashCode() : 0);
    result = 31 * result + (lastDeployedById != null ? lastDeployedById.hashCode() : 0);
    result = 31 * result + (lastDeployedByName != null ? lastDeployedByName.hashCode() : 0);
    result = 31 * result + (int) (lastDeployedAt ^ (lastDeployedAt >>> 32));
    result = 31 * result + (lastWorkflowId != null ? lastWorkflowId.hashCode() : 0);
    result = 31 * result + (lastWorkflowName != null ? lastWorkflowName.hashCode() : 0);
    result = 31 * result + (lastPipelineId != null ? lastPipelineId.hashCode() : 0);
    result = 31 * result + (lastPipelineName != null ? lastPipelineName.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  public static final class Builder {
    protected String appId;
    private String envId;
    private String envName;
    private EnvironmentType envType;
    private String accountId;
    private String serviceId;
    private String serviceName;
    private String appName;
    private String hostId;
    private String hostName;
    private String uuid;
    private String hostPublicDns;
    private String infraMappingId;
    private EmbeddedUser createdBy;
    private String infraMappingType;
    private String computeProviderId;
    private long createdAt;
    private String computeProviderName;
    private EmbeddedUser lastUpdatedBy;
    private String lastArtifactStreamId;
    private long lastUpdatedAt;
    private String lastArtifactId;
    private String lastArtifactName;
    private String lastArtifactSourceName;
    private String lastArtifactBuildNum;
    private String lastDeployedById;
    private String lastDeployedByName;
    private long lastDeployedAt;
    private String lastWorkflowId;
    private String lastWorkflowName;
    private String lastPipelineId;
    private String lastPipelineName;
    private InstanceMetadata metadata;

    private Builder() {}

    public static Builder anInstance() {
      return new Builder();
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

    public Builder withHostId(String hostId) {
      this.hostId = hostId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withHostPublicDns(String hostPublicDns) {
      this.hostPublicDns = hostPublicDns;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
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

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastArtifactStreamId(String lastArtifactStreamId) {
      this.lastArtifactStreamId = lastArtifactStreamId;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
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

    public Builder withLastWorkflowId(String lastWorkflowId) {
      this.lastWorkflowId = lastWorkflowId;
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

    public Builder withMetadata(InstanceMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public Instance build() {
      Instance instance = new Instance();
      instance.setEnvId(envId);
      instance.setEnvName(envName);
      instance.setEnvType(envType);
      instance.setAccountId(accountId);
      instance.setServiceId(serviceId);
      instance.setServiceName(serviceName);
      instance.setAppName(appName);
      instance.setHostId(hostId);
      instance.setHostName(hostName);
      instance.setUuid(uuid);
      instance.setHostPublicDns(hostPublicDns);
      instance.setAppId(appId);
      instance.setInfraMappingId(infraMappingId);
      instance.setCreatedBy(createdBy);
      instance.setInfraMappingType(infraMappingType);
      instance.setComputeProviderId(computeProviderId);
      instance.setCreatedAt(createdAt);
      instance.setComputeProviderName(computeProviderName);
      instance.setLastUpdatedBy(lastUpdatedBy);
      instance.setLastArtifactStreamId(lastArtifactStreamId);
      instance.setLastUpdatedAt(lastUpdatedAt);
      instance.setLastArtifactId(lastArtifactId);
      instance.setLastArtifactName(lastArtifactName);
      instance.setLastArtifactSourceName(lastArtifactSourceName);
      instance.setLastArtifactBuildNum(lastArtifactBuildNum);
      instance.setLastDeployedById(lastDeployedById);
      instance.setLastDeployedByName(lastDeployedByName);
      instance.setLastDeployedAt(lastDeployedAt);
      instance.setLastWorkflowId(lastWorkflowId);
      instance.setLastWorkflowName(lastWorkflowName);
      instance.setLastPipelineId(lastPipelineId);
      instance.setLastPipelineName(lastPipelineName);
      instance.setMetadata(metadata);
      return instance;
    }
  }
}
