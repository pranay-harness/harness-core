package software.wings.infra;

import static software.wings.beans.AzureKubernetesInfrastructureMapping.Builder.anAzureKubernetesInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

@JsonTypeName("AZURE_KUBERNETES")
@Data
@Builder
public class AzureKubernetesService
    implements KubernetesInfrastructure, InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String clusterName;

  private String namespace;

  private String releaseName;

  private String subscriptionId;

  private String resourceGroup;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return anAzureKubernetesInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withClusterName(clusterName)
        .withNamespace(namespace)
        .withReleaseName(releaseName)
        .withSubscriptionId(subscriptionId)
        .withResourceGroup(resourceGroup)
        .withInfraMappingType(InfrastructureMappingType.AZURE_KUBERNETES.name())
        .build();
  }

  @Override
  public Class<AzureKubernetesInfrastructureMapping> getMappingClass() {
    return AzureKubernetesInfrastructureMapping.class;
  }

  public String getCloudProviderInfrastructureType() {
    return AZURE_KUBERNETES;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AZURE_KUBERNETES)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String clusterName;
    private String namespace;
    private String releaseName;
    private String resourceGroup;
    private String subscriptionId;

    @Builder
    public Yaml(String type, String cloudProviderName, String clusterName, String namespace, String releaseName,
        String resourceGroup, String subscriptionId) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setClusterName(clusterName);
      setNamespace(namespace);
      setReleaseName(releaseName);
      setResourceGroup(resourceGroup);
      setSubscriptionId(subscriptionId);
    }

    public Yaml() {
      super(AZURE_KUBERNETES);
    }
  }
}
