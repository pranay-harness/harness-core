package software.wings.infra;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.Map;
import java.util.Set;

@JsonTypeName("GCP_KUBERNETES")
@Data
@Builder
@FieldNameConstants(innerTypeName = "GoogleKubernetesEngineKeys")
public class GoogleKubernetesEngine
    implements KubernetesInfrastructure, InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  @ExcludeFieldMap private String cloudProviderId;
  private String clusterName;
  private String namespace;
  private String releaseName;
  @ExcludeFieldMap private Map<String, String> expressions;

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

  public String getInfrastructureType() {
    return GCP_KUBERNETES_ENGINE;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.GCP;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    return ImmutableSet.of(GoogleKubernetesEngineKeys.clusterName, GoogleKubernetesEngineKeys.namespace,
        GoogleKubernetesEngineKeys.releaseName);
  }

  @Override
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(GCP_KUBERNETES_ENGINE)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String clusterName;
    private String namespace;
    private String releaseName;
    private Map<String, String> expressions;

    @Builder
    public Yaml(String type, String cloudProviderName, String clusterName, String namespace, String releaseName,
        Map<String, String> expressions) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setClusterName(clusterName);
      setNamespace(namespace);
      setReleaseName(releaseName);
      setExpressions(expressions);
    }

    public Yaml() {
      super(GCP_KUBERNETES_ENGINE);
    }
  }
}
