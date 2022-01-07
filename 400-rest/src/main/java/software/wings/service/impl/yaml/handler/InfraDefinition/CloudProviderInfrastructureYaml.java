/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.AZURE_VMSS;
import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;
import static software.wings.beans.InfrastructureType.CODE_DEPLOY;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.InfrastructureType;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.AzureVMSSInfra;
import software.wings.infra.AzureWebAppInfra;
import software.wings.infra.CodeDeployInfrastructure;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.yaml.BaseYamlWithType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsAmiInfrastructure.Yaml.class, name = AWS_AMI)
  , @JsonSubTypes.Type(value = AwsEcsInfrastructure.Yaml.class, name = AWS_ECS),
      @JsonSubTypes.Type(value = AwsInstanceInfrastructure.Yaml.class, name = AWS_INSTANCE),
      @JsonSubTypes.Type(value = AwsLambdaInfrastructure.Yaml.class, name = AWS_LAMBDA),
      @JsonSubTypes.Type(value = AzureKubernetesService.Yaml.class, name = AZURE_KUBERNETES),
      @JsonSubTypes.Type(value = AzureInstanceInfrastructure.Yaml.class, name = AZURE_SSH),
      @JsonSubTypes.Type(value = CodeDeployInfrastructure.Yaml.class, name = CODE_DEPLOY),
      @JsonSubTypes.Type(value = DirectKubernetesInfrastructure.Yaml.class, name = DIRECT_KUBERNETES),
      @JsonSubTypes.Type(value = GoogleKubernetesEngine.Yaml.class, name = GCP_KUBERNETES_ENGINE),
      @JsonSubTypes.Type(value = PcfInfraStructure.Yaml.class, name = PCF_INFRASTRUCTURE),
      @JsonSubTypes.Type(value = PhysicalInfra.Yaml.class, name = PHYSICAL_INFRA),
      @JsonSubTypes.Type(value = PhysicalInfraWinrm.Yaml.class, name = PHYSICAL_INFRA_WINRM),
      @JsonSubTypes.Type(value = AzureVMSSInfra.Yaml.class, name = AZURE_VMSS),
      @JsonSubTypes.Type(value = AzureWebAppInfra.Yaml.class, name = AZURE_WEBAPP),
      @JsonSubTypes.Type(value = CustomInfrastructure.Yaml.class, name = InfrastructureType.CUSTOM_INFRASTRUCTURE)
})
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public abstract class CloudProviderInfrastructureYaml extends BaseYamlWithType {
  public CloudProviderInfrastructureYaml(String type) {
    super(type);
  }
}
