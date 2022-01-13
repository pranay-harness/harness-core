/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_VMSS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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
@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AZURE_VMSS)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
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
    public Yaml(String type, String cloudProviderName, String baseVMSSName, String userName, String resourceGroupName,
        String subscriptionId, String passwordSecretTextName, String hostConnectionAttrs, VMSSAuthType vmssAuthType,
        VMSSDeploymentType vmssDeploymentType) {
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

    public Yaml() {
      super(AZURE_VMSS);
    }
  }
}
