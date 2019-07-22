package software.wings.infra;

import static software.wings.beans.AzureKubernetesInfrastructureMapping.Builder.anAzureKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AZURE_KUBERNETES")
@Data
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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }
}
