package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_VMSS;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.VMSSAuthType;
import software.wings.beans.VMSSDeploymentType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("AZURE_VMSS")
@Data
@Builder
@FieldNameConstants(innerTypeName = "AzureVMSSInfraKeys")
public class AzureVMSSInfra implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  private String cloudProviderId;
  private String baseVMSSName;
  private String userName;
  private String resourceGroupName;
  private String subscriptionId;
  private String passwordSecretTextName;
  private String hostConnectionAttrs;
  private VMSSAuthType vmssAuthType;
  @IncludeFieldMap private VMSSDeploymentType vmssDeploymentType;

  @Override
  public InfrastructureMapping getInfraMapping() {
    AzureVMSSInfrastructureMapping infrastructureMapping = AzureVMSSInfrastructureMapping.builder()
                                                               .baseVMSSName(baseVMSSName)
                                                               .userName(userName)
                                                               .resourceGroupName(resourceGroupName)
                                                               .subscriptionId(subscriptionId)
                                                               .passwordSecretTextName(passwordSecretTextName)
                                                               .hostConnectionAttrs(hostConnectionAttrs)
                                                               .vmssAuthType(vmssAuthType)
                                                               .vmssDeploymentType(vmssDeploymentType)
                                                               .build();
    infrastructureMapping.setComputeProviderSettingId(cloudProviderId);
    return infrastructureMapping;
  }

  @Override
  public Class<AzureVMSSInfrastructureMapping> getMappingClass() {
    return AzureVMSSInfrastructureMapping.class;
  }

  @Override
  public String getInfrastructureType() {
    return AZURE_VMSS;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }
}
