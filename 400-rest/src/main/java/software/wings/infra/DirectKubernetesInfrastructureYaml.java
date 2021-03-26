package software.wings.infra;

import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(DIRECT_KUBERNETES)
public final class DirectKubernetesInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String clusterName;
  private String namespace;
  private String releaseName;
  private Map<String, String> expressions;

  @Builder
  public DirectKubernetesInfrastructureYaml(String type, String cloudProviderName, String clusterName, String namespace,
      String releaseName, Map<String, String> expressions) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setClusterName(clusterName);
    setNamespace(namespace);
    setReleaseName(releaseName);
    setExpressions(expressions);
  }

  public DirectKubernetesInfrastructureYaml() {
    super(DIRECT_KUBERNETES);
  }
}
