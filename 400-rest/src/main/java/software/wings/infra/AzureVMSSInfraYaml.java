package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_VMSS;

import software.wings.beans.VMSSAuthType;
import software.wings.beans.VMSSDeploymentType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AZURE_VMSS)
public final class AzureVMSSInfraYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String baseVMSSName;
  private String userName;
  private String resourceGroupName;
  private String subscriptionId;
  private String passwordSecretTextName;
  private String hostConnectionAttrs;
  private VMSSAuthType vmssAuthType;
  private VMSSDeploymentType vmssDeploymentType;

  @Builder
  public AzureVMSSInfraYaml(String type, String cloudProviderName, String baseVMSSName, String userName,
      String resourceGroupName, String subscriptionId, String passwordSecretTextName, String hostConnectionAttrs,
      VMSSAuthType vmssAuthType, VMSSDeploymentType vmssDeploymentType) {
    super(type);
    this.cloudProviderName = cloudProviderName;
    this.baseVMSSName = baseVMSSName;
    this.userName = userName;
    this.resourceGroupName = resourceGroupName;
    this.subscriptionId = subscriptionId;
    this.passwordSecretTextName = passwordSecretTextName;
    this.hostConnectionAttrs = hostConnectionAttrs;
    this.vmssAuthType = vmssAuthType;
    this.vmssDeploymentType = vmssDeploymentType;
  }

  public AzureVMSSInfraYaml() {
    super(AZURE_VMSS);
  }
}
