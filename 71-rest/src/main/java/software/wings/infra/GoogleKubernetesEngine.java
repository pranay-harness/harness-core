package software.wings.infra;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("GCP_KUBERNETES")
@Data
public class GoogleKubernetesEngine implements KubernetesInfrastructure, InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap private String clusterName;

  @IncludeInFieldMap private String namespace;

  @IncludeInFieldMap private String releaseName;

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
}
