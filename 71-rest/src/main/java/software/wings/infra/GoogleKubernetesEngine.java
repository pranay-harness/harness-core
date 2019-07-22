package software.wings.infra;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("GCP_KUBERNETES")
@Data
@Builder
public class GoogleKubernetesEngine
    implements KubernetesInfrastructure, InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String clusterName;

  private String namespace;

  private String releaseName;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aGcpKubernetesInfrastructureMapping()
        .withClusterName(clusterName)
        .withComputeProviderSettingId(cloudProviderId)
        .withNamespace(namespace)
        .withReleaseName(releaseName)
        .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.name())
        .build();
  }

  @Override
  public Class<GcpKubernetesInfrastructureMapping> getMappingClass() {
    return GcpKubernetesInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.GCP;
  }
}
