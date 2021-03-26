package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GcpKubernetesInfrastructureMappingYaml
    extends ContainerInfrastructureMappingYamlWithComputeProvider {
  private String namespace;
  private String releaseName;

  @lombok.Builder
  public GcpKubernetesInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String cluster,
      String namespace, String releaseName, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, cluster, blueprints);
    this.namespace = namespace;
    this.releaseName = releaseName;
  }
}
