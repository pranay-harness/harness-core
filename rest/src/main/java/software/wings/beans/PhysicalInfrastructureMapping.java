package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
public class PhysicalInfrastructureMapping extends PhysicalInfrastructureMappingBase {
  private String hostConnectionAttrs;

  public PhysicalInfrastructureMapping() {
    super(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH);
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(
        format("%s (DataCenter_SSH)", Optional.ofNullable(this.getComputeProviderName()).orElse("data-center-ssh")));
  }

  @Override
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends PhysicalInfrastructureMappingBase.Yaml {
    // maps to hostConnectionAttrs
    // This would either be a username/password / ssh key id
    private String connection;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String name, List<String> hostNames,
        String loadBalancer, String connection) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, name, hostNames, loadBalancer);
      this.connection = connection;
    }
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    protected String accountId;
    private String hostConnectionAttrs;
    private List<String> hostNames;
    private String loadBalancerName;
    private String loadBalancerId;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    private boolean autoPopulate = true;

    private Builder() {}

    public static PhysicalInfrastructureMapping.Builder aPhysicalInfrastructureMapping() {
      return new PhysicalInfrastructureMapping.Builder();
    }

    public PhysicalInfrastructureMapping.Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withName(String name) {
      this.name = name;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder but() {
      return aPhysicalInfrastructureMapping()
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withHostNames(hostNames)
          .withUuid(uuid)
          .withAppId(appId)
          .withAccountId(accountId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withEntityYamlPath(entityYamlPath)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withInfraMappingType(infraMappingType)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withName(name)
          .withAutoPopulate(autoPopulate)
          .withAccountId(accountId);
    }

    public PhysicalInfrastructureMapping build() {
      PhysicalInfrastructureMapping physicalInfrastructureMapping = new PhysicalInfrastructureMapping();
      physicalInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      physicalInfrastructureMapping.setHostNames(hostNames);
      physicalInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      physicalInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      physicalInfrastructureMapping.setUuid(uuid);
      physicalInfrastructureMapping.setAppId(appId);
      physicalInfrastructureMapping.setAccountId(accountId);
      physicalInfrastructureMapping.setCreatedBy(createdBy);
      physicalInfrastructureMapping.setCreatedAt(createdAt);
      physicalInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      physicalInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      physicalInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      physicalInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      physicalInfrastructureMapping.setEnvId(envId);
      physicalInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMapping.setServiceId(serviceId);
      physicalInfrastructureMapping.setComputeProviderType(computeProviderType);
      physicalInfrastructureMapping.setInfraMappingType(infraMappingType);
      physicalInfrastructureMapping.setDeploymentType(deploymentType);
      physicalInfrastructureMapping.setComputeProviderName(computeProviderName);
      physicalInfrastructureMapping.setName(name);
      physicalInfrastructureMapping.setAutoPopulate(autoPopulate);
      physicalInfrastructureMapping.setAccountId(accountId);
      return physicalInfrastructureMapping;
    }
  }
}
