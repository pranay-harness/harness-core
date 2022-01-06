/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.utils.Utils;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureVMSSInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AzureVMSSInfrastructureMapping extends InfrastructureMapping {
  private String baseVMSSName;
  private String userName;
  private String resourceGroupName;
  private String subscriptionId;
  private String passwordSecretTextName;
  private String hostConnectionAttrs;
  private VMSSAuthType vmssAuthType;
  private VMSSDeploymentType vmssDeploymentType;

  public AzureVMSSInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_VMSS.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("(AZURE_VMSS) %s",
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  @Builder
  public AzureVMSSInfrastructureMapping(String baseVMSSName, String userName, String resourceGroupName,
      String subscriptionId, String passwordSecretTextName, String hostConnectionAttrs, VMSSAuthType vmssAuthType,
      VMSSDeploymentType vmssDeploymentType) {
    super(InfrastructureMappingType.AZURE_VMSS.name());
    this.baseVMSSName = baseVMSSName;
    this.userName = userName;
    this.resourceGroupName = resourceGroupName;
    this.subscriptionId = subscriptionId;
    this.passwordSecretTextName = passwordSecretTextName;
    this.hostConnectionAttrs = hostConnectionAttrs;
    this.vmssAuthType = vmssAuthType;
    this.vmssDeploymentType = vmssDeploymentType;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends YamlWithComputeProvider {
    private String baseVMSSName;
    private String userName;
    private String resourceGroupName;
    private String subscriptionId;
    private String passwordSecretTextName;
    private String hostConnectionAttrs;
    private VMSSAuthType vmssAuthType;
    private VMSSDeploymentType vmssDeploymentType;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String baseVMSSName, String userName, String resourceGroupName, String subscriptionId,
        String passwordSecretTextName, String hostConnectionAttrs, VMSSAuthType vmssAuthType,
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
}
