/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessModule._870_CG_YAML;
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
@TargetModule(_870_CG_YAML)
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
