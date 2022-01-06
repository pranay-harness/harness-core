/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDC)
@TargetModule(_955_CG_YAML)
public final class InfrastructureDefinitionYaml extends BaseEntityYaml {
  private String name;
  private CloudProviderType cloudProviderType;
  private DeploymentType deploymentType;
  @NotNull private List<CloudProviderInfrastructureYaml> infrastructure = new ArrayList<>();
  private List<String> scopedServices;
  private String provisioner;

  /*
   Support for Custom Deployment
    */
  private String deploymentTypeTemplateUri;

  @Builder
  public InfrastructureDefinitionYaml(String type, String harnessApiVersion, CloudProviderType cloudProviderType,
      DeploymentType deploymentType, List<CloudProviderInfrastructureYaml> infrastructure, List<String> scopedServices,
      String provisioner, String deploymentTypeTemplateUri) {
    super(type, harnessApiVersion);
    setCloudProviderType(cloudProviderType);
    setDeploymentType(deploymentType);
    setInfrastructure(infrastructure);
    setScopedServices(scopedServices);
    setProvisioner(provisioner);
    setDeploymentTypeTemplateUri(deploymentTypeTemplateUri);
  }
}
