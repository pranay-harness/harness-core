/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.AZURE_VMSS;
import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;
import static software.wings.beans.InfrastructureType.CODE_DEPLOY;
import static software.wings.beans.InfrastructureType.CUSTOM_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.InfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Collections;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsAmiInfrastructure.class, name = AWS_AMI)
  , @JsonSubTypes.Type(value = AwsEcsInfrastructure.class, name = AWS_ECS),
      @JsonSubTypes.Type(value = AwsInstanceInfrastructure.class, name = AWS_INSTANCE),
      @JsonSubTypes.Type(value = AwsLambdaInfrastructure.class, name = AWS_LAMBDA),
      @JsonSubTypes.Type(value = AzureKubernetesService.class, name = AZURE_KUBERNETES),
      @JsonSubTypes.Type(value = AzureInstanceInfrastructure.class, name = AZURE_SSH),
      @JsonSubTypes.Type(value = AzureVMSSInfra.class, name = AZURE_VMSS),
      @JsonSubTypes.Type(value = AzureWebAppInfra.class, name = AZURE_WEBAPP),
      @JsonSubTypes.Type(value = CodeDeployInfrastructure.class, name = CODE_DEPLOY),
      @JsonSubTypes.Type(value = DirectKubernetesInfrastructure.class, name = DIRECT_KUBERNETES),
      @JsonSubTypes.Type(value = GoogleKubernetesEngine.class, name = GCP_KUBERNETES_ENGINE),
      @JsonSubTypes.Type(value = PcfInfraStructure.class, name = PCF_INFRASTRUCTURE),
      @JsonSubTypes.Type(value = PhysicalInfra.class, name = PHYSICAL_INFRA),
      @JsonSubTypes.Type(value = PhysicalInfraWinrm.class, name = PHYSICAL_INFRA_WINRM),
      @JsonSubTypes.Type(value = CustomInfrastructure.class, name = CUSTOM_INFRASTRUCTURE)
})
@OwnedBy(CDC)
@TargetModule(_957_CG_BEANS)
public interface InfraMappingInfrastructureProvider extends CloudProviderInfrastructure {
  @JsonIgnore InfrastructureMapping getInfraMapping();

  @JsonIgnore Class<? extends InfrastructureMapping> getMappingClass();

  @JsonIgnore
  default Set<String> getUserDefinedUniqueInfraFields() {
    return Collections.emptySet();
  }
}
