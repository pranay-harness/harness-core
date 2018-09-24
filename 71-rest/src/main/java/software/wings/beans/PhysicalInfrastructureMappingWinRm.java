package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonTypeName("PHYSICAL_DATA_CENTER_WINRM")
public class PhysicalInfrastructureMappingWinRm extends PhysicalInfrastructureMappingBase {
  @NotEmpty private String winRmConnectionAttributes;

  public PhysicalInfrastructureMappingWinRm() {
    super(InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM);
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(format(
        "%s (DataCenter_WinRM)", Optional.ofNullable(this.getComputeProviderName()).orElse("data-center-winrm")));
  }

  public String getWinRmConnectionAttributes() {
    return winRmConnectionAttributes;
  }

  public void setWinRmConnectionAttributes(String winRmConnectionAttributes) {
    this.winRmConnectionAttributes = winRmConnectionAttributes;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends PhysicalInfrastructureMappingBase.Yaml {
    private String winRmProfile;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String name, List<String> hostNames,
        String loadBalancer, String winRmProfile) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, name, hostNames, loadBalancer);
      this.winRmProfile = winRmProfile;
    }
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    protected String accountId;
    private String winRmConnectionAttributes;
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

    public static PhysicalInfrastructureMappingWinRm.Builder aPhysicalInfrastructureMappingWinRm() {
      return new PhysicalInfrastructureMappingWinRm.Builder();
    }

    public PhysicalInfrastructureMappingWinRm.Builder withWinRmConnectionAttributes(String winRmConnectionAttrs) {
      this.winRmConnectionAttributes = winRmConnectionAttrs;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withName(String name) {
      this.name = name;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm.Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public PhysicalInfrastructureMappingWinRm build() {
      PhysicalInfrastructureMappingWinRm physicalInfrastructureMappingWinRm = new PhysicalInfrastructureMappingWinRm();
      physicalInfrastructureMappingWinRm.setWinRmConnectionAttributes(winRmConnectionAttributes);
      physicalInfrastructureMappingWinRm.setHostNames(hostNames);
      physicalInfrastructureMappingWinRm.setLoadBalancerName(loadBalancerName);
      physicalInfrastructureMappingWinRm.setLoadBalancerId(loadBalancerId);
      physicalInfrastructureMappingWinRm.setUuid(uuid);
      physicalInfrastructureMappingWinRm.setAppId(appId);
      physicalInfrastructureMappingWinRm.setAccountId(accountId);
      physicalInfrastructureMappingWinRm.setCreatedBy(createdBy);
      physicalInfrastructureMappingWinRm.setCreatedAt(createdAt);
      physicalInfrastructureMappingWinRm.setLastUpdatedBy(lastUpdatedBy);
      physicalInfrastructureMappingWinRm.setLastUpdatedAt(lastUpdatedAt);
      physicalInfrastructureMappingWinRm.setEntityYamlPath(entityYamlPath);
      physicalInfrastructureMappingWinRm.setComputeProviderSettingId(computeProviderSettingId);
      physicalInfrastructureMappingWinRm.setEnvId(envId);
      physicalInfrastructureMappingWinRm.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMappingWinRm.setServiceId(serviceId);
      physicalInfrastructureMappingWinRm.setComputeProviderType(computeProviderType);
      physicalInfrastructureMappingWinRm.setInfraMappingType(infraMappingType);
      physicalInfrastructureMappingWinRm.setDeploymentType(deploymentType);
      physicalInfrastructureMappingWinRm.setComputeProviderName(computeProviderName);
      physicalInfrastructureMappingWinRm.setName(name);
      physicalInfrastructureMappingWinRm.setAutoPopulate(autoPopulate);
      physicalInfrastructureMappingWinRm.setAccountId(accountId);
      return physicalInfrastructureMappingWinRm;
    }
  }
}
