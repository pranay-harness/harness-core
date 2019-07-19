package software.wings.infra;

import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.ContainerInfrastructureMapping.ContainerInfrastructureMappingKeys;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping.DirectKubernetesInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("DIRECT_KUBERNETES")
@Data
public class DirectKubernetesInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = ContainerInfrastructureMappingKeys.clusterName) private String clusterName;

  @IncludeInFieldMap(key = DirectKubernetesInfrastructureMappingKeys.namespace) private String namespace;

  @IncludeInFieldMap(key = DirectKubernetesInfrastructureMappingKeys.releaseName) @Trimmed private String releaseName;

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
}
