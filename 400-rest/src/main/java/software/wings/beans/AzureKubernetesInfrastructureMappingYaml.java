package software.wings.beans;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureKubernetesInfrastructureMappingYaml extends YamlWithComputeProviderYaml {
  private String subscriptionId;
  private String resourceGroup;
  private String namespace;
  private String releaseName;

  @lombok.Builder
  public AzureKubernetesInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String cluster,
      String subscriptionId, String resourceGroup, String namespace, String releaseName,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, cluster, blueprints);
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
    this.namespace = namespace;
    this.releaseName = releaseName;
  }
}
