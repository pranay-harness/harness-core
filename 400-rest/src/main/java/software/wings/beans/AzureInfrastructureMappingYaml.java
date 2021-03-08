package software.wings.beans;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureInfrastructureMappingYaml extends YamlWithComputeProvider {
  private String subscriptionId;
  private String resourceGroup;
  private List<AzureTag> azureTags;

  @lombok.Builder
  public AzureInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName,
      String subscriptionId, String resourceGroup, List<AzureTag> azureTags, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
        computeProviderName, blueprints);
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
    this.azureTags = azureTags;
  }
}
