package software.wings.infra;

import static java.lang.String.format;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;

import com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;
import software.wings.utils.Validator;

import java.util.Map;
import java.util.Set;

@JsonTypeName("GCP_KUBERNETES")
@Data
@Builder
@FieldNameConstants(innerTypeName = "GoogleKubernetesEngineKeys")
public class GoogleKubernetesEngine
    implements KubernetesInfrastructure, InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  private String cloudProviderId;
  @IncludeFieldMap private String clusterName;
  private String namespace;
  private String releaseName;
  private Map<String, String> expressions;

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
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    for (Map.Entry<String, Object> entry : resolvedExpressions.entrySet()) {
      switch (entry.getKey()) {
        case "clusterName":
          Validator.ensureType(String.class, entry.getValue(), "Region should be of String type");
          setClusterName((String) entry.getValue());
          break;
        case "namespace":
          Validator.ensureType(String.class, entry.getValue(), "Namespace should be of String type");
          setNamespace((String) entry.getValue());
          break;
        case "releaseName":
          Validator.ensureType(String.class, entry.getValue(), "Release name should be of String type");
          setReleaseName((String) entry.getValue());
          break;
        default:
          throw new InvalidRequestException(format("Unknown expression : [%s]", entry.getKey()));
      }
    }
    if (getClusterName() == null) {
      throw new InvalidRequestException("Cluster Name is mandatory");
    }
  }

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
