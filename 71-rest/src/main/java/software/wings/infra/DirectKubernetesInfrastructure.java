package software.wings.infra;

import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("DIRECT_KUBERNETES")
@Data
public class DirectKubernetesInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String clusterName;

  private String namespace;

  @Trimmed private String releaseName;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aDirectKubernetesInfrastructureMapping()
        .withClusterName(clusterName)
        .withNamespace(namespace)
        .withReleaseName(releaseName)
        .withComputeProviderSettingId(cloudProviderId)
        .withInfraMappingType(InfrastructureMappingType.DIRECT_KUBERNETES.name())
        .build();
  }

  @Override
  public Class<DirectKubernetesInfrastructureMapping> getMappingClass() {
    return DirectKubernetesInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.KUBERNETES_CLUSTER;
  }
}
