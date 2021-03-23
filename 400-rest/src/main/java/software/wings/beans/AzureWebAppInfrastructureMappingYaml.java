package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppInfrastructureMappingYaml extends InfrastructureMappingYamlWithComputeProvider {
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
