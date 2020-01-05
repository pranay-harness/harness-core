package software.wings.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.common.InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.Trimmed;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

@JsonTypeName("DIRECT_KUBERNETES")
@Data
@Builder
public class DirectKubernetesInfrastructure
    implements InfraMappingInfrastructureProvider, KubernetesInfrastructure, FieldKeyValMapProvider {
  private String cloudProviderId;
  @IncludeFieldMap private String clusterName;
  @IncludeFieldMap @Expression private String namespace;
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
  public String getReleaseName() {
    return isEmpty(releaseName) ? INFRA_KUBERNETES_INFRAID_EXPRESSION : releaseName;
  }

  @Override
  public Class<DirectKubernetesInfrastructureMapping> getMappingClass() {
    return DirectKubernetesInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.KUBERNETES_CLUSTER;
  }

  @Override
  public String getInfrastructureType() {
    return DIRECT_KUBERNETES;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(DIRECT_KUBERNETES)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String clusterName;
    private String namespace;
    private String releaseName;

    @Builder
    public Yaml(String type, String cloudProviderName, String clusterName, String namespace, String releaseName) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setClusterName(clusterName);
      setNamespace(namespace);
      setReleaseName(releaseName);
    }

    public Yaml() {
      super(DIRECT_KUBERNETES);
    }
  }
}
