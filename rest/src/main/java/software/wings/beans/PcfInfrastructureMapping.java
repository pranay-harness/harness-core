package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.utils.Util;

import java.util.List;
import java.util.Optional;

@JsonTypeName("PCF")
@Data
@EqualsAndHashCode(callSuper = true)
public class PcfInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Organization", required = true) private String organization;
  @Attributes(title = "Space", required = true) private String space;
  @Attributes(title = "Temporary Route Map") private List<String> tempRouteMap;
  @Attributes(title = "Route Maps", required = true) private List<String> routeMaps;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public PcfInfrastructureMapping() {
    super(InfrastructureMappingType.PCF.name());
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(String.format("%s (%s_%s::%s) %s", this.getOrganization(), this.getComputeProviderType(),
        this.getDeploymentType(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getSpace()));
  }

  @Override
  public String getHostConnectionAttrs() {
    return null;
  }

  public static Builder anPcfInfrastructureMapping() {
    return new Builder();
  }
  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String type;
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
    // auto populate name
    private boolean autoPopulate = true;
    private String organization;
    private String space;
    private List<String> tempRouteMap;
    private List<String> routeMaps;

    private Builder() {}

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public Builder withOrganization(String organization) {
      this.organization = organization;
      return this;
    }

    public Builder withSpace(String space) {
      this.space = space;
      return this;
    }

    public Builder withTempRouteMap(List<String> tempRouteMap) {
      this.tempRouteMap = tempRouteMap;
      return this;
    }

    public Builder withRouteMaps(List<String> routeMaps) {
      this.routeMaps = routeMaps;
      return this;
    }

    public Builder but() {
      return anPcfInfrastructureMapping()
          .withType(type)
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
          .withOrganization(organization)
          .withSpace(space)
          .withTempRouteMap(tempRouteMap)
          .withRouteMaps(routeMaps);
    }

    public PcfInfrastructureMapping build() {
      PcfInfrastructureMapping pcfInfrastructureMapping = new PcfInfrastructureMapping();
      pcfInfrastructureMapping.setUuid(uuid);
      pcfInfrastructureMapping.setAppId(appId);
      pcfInfrastructureMapping.setCreatedBy(createdBy);
      pcfInfrastructureMapping.setCreatedAt(createdAt);
      pcfInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      pcfInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      pcfInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      pcfInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      pcfInfrastructureMapping.setEnvId(envId);
      pcfInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      pcfInfrastructureMapping.setServiceId(serviceId);
      pcfInfrastructureMapping.setComputeProviderType(computeProviderType);
      pcfInfrastructureMapping.setInfraMappingType(infraMappingType);
      pcfInfrastructureMapping.setDeploymentType(deploymentType);
      pcfInfrastructureMapping.setComputeProviderName(computeProviderName);
      pcfInfrastructureMapping.setName(name);
      pcfInfrastructureMapping.setAutoPopulate(autoPopulate);
      pcfInfrastructureMapping.setAccountId(accountId);
      pcfInfrastructureMapping.setOrganization(organization);
      pcfInfrastructureMapping.setSpace(space);
      pcfInfrastructureMapping.setTempRouteMap(tempRouteMap);
      pcfInfrastructureMapping.setRouteMaps(routeMaps);
      return pcfInfrastructureMapping;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends YamlWithComputeProvider {
    private String organization;
    private String space;
    private List<String> tempRouteMap;
    private List<String> routeMaps;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String organization, String space,
        List<String> tempRouteMap, List<String> routeMaps) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName);
      this.organization = organization;
      this.space = space;
      this.tempRouteMap = tempRouteMap;
      this.routeMaps = routeMaps;
    }
  }
}
