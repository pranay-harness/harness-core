package software.wings.beans;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppInfrastructureMappingYaml extends YamlWithComputeProvider {
  private String subscriptionId;
  private String resourceGroup;

  public AzureWebAppInfrastructureMappingYaml(String type, String harnessApiVersion, String serviceName,
      String infraMappingType, String deploymentType, String computeProviderType, String computeProviderName,
      Map<String, Object> blueprints, String subscriptionId, String resourceGroup) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
        computeProviderName, blueprints);
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
  }
}
