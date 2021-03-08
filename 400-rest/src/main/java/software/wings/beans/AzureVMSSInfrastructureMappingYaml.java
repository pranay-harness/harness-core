package software.wings.beans;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureVMSSInfrastructureMappingYaml extends YamlWithComputeProvider {
  private String baseVMSSName;
  private String userName;
  private String resourceGroupName;
  private String subscriptionId;
  private String passwordSecretTextName;
  private String hostConnectionAttrs;
  private VMSSAuthType vmssAuthType;
  private VMSSDeploymentType vmssDeploymentType;

  public AzureVMSSInfrastructureMappingYaml(String type, String harnessApiVersion, String serviceName,
      String infraMappingType, String deploymentType, String computeProviderType, String computeProviderName,
      Map<String, Object> blueprints, String baseVMSSName, String userName, String resourceGroupName,
      String subscriptionId, String passwordSecretTextName, String hostConnectionAttrs, VMSSAuthType vmssAuthType,
      VMSSDeploymentType vmssDeploymentType) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
        computeProviderName, blueprints);
    this.baseVMSSName = baseVMSSName;
    this.userName = userName;
    this.resourceGroupName = resourceGroupName;
    this.subscriptionId = subscriptionId;
    this.passwordSecretTextName = passwordSecretTextName;
    this.hostConnectionAttrs = hostConnectionAttrs;
    this.vmssAuthType = vmssAuthType;
    this.vmssDeploymentType = vmssDeploymentType;
  }
}
