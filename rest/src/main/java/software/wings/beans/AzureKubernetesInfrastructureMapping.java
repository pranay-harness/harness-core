package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.Map;
import java.util.Optional;

@JsonTypeName("AZURE_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class AzureKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "SubscriptionId") private String subscriptionId;
  @Attributes(title = "Resource Group") private String resourceGroup;
  @Attributes(title = "Namespace") private String namespace;

  public AzureKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_KUBERNETES.name());
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(format("%s_AZURE_Kubernetes_%s_%s", this.getClusterName(),
        Optional.ofNullable(this.getComputeProviderName())
            .orElse(this.getComputeProviderType().toLowerCase())
            .replace(':', '_'),
        Optional.ofNullable(this.getNamespace()).orElse("default")));
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
    private String subscriptionId;
    private String resourceGroup;
    private String namespace;
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

    private Builder() {}

    public static Builder anAzureKubernetesInfrastructureMapping() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public Builder withResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
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

    public AzureKubernetesInfrastructureMapping build() {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          new AzureKubernetesInfrastructureMapping();
      azureKubernetesInfrastructureMapping.setClusterName(clusterName);
      azureKubernetesInfrastructureMapping.setSubscriptionId(subscriptionId);
      azureKubernetesInfrastructureMapping.setResourceGroup(resourceGroup);
      azureKubernetesInfrastructureMapping.setNamespace(namespace);
      azureKubernetesInfrastructureMapping.setUuid(uuid);
      azureKubernetesInfrastructureMapping.setAppId(appId);
      azureKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      azureKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      azureKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      azureKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      azureKubernetesInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      azureKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      azureKubernetesInfrastructureMapping.setEnvId(envId);
      azureKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      azureKubernetesInfrastructureMapping.setServiceId(serviceId);
      azureKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      azureKubernetesInfrastructureMapping.setInfraMappingType(infraMappingType);
      azureKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      azureKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      azureKubernetesInfrastructureMapping.setName(name);
      azureKubernetesInfrastructureMapping.setAutoPopulate(autoPopulate);
      azureKubernetesInfrastructureMapping.setAccountId(accountId);
      return azureKubernetesInfrastructureMapping;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends YamlWithComputeProvider {
    private String subscriptionId;
    private String resourceGroup;
    private String namespace;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster,
        String subscriptionId, String resourceGroup, String namespace) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster);
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
      this.namespace = namespace;
    }
  }
}
