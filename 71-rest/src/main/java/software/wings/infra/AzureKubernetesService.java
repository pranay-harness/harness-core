package software.wings.infra;

import static software.wings.beans.AzureKubernetesInfrastructureMapping.Builder.anAzureKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping.AzureKubernetesInfrastructureMappingKeys;
import software.wings.beans.ContainerInfrastructureMapping.ContainerInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AZURE_KUBERNETES")
@Data
public class AzureKubernetesService implements KubernetesInfrastructure, InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = ContainerInfrastructureMappingKeys.clusterName) private String clusterName;

  @IncludeInFieldMap(key = AzureKubernetesInfrastructureMappingKeys.namespace) private String namespace;

  @IncludeInFieldMap(key = AzureKubernetesInfrastructureMappingKeys.releaseName) private String releaseName;

  @IncludeInFieldMap(key = AzureKubernetesInfrastructureMappingKeys.subscriptionId) private String subscriptionId;

  @IncludeInFieldMap(key = AzureKubernetesInfrastructureMappingKeys.resourceGroup) private String resourceGroup;

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
}
