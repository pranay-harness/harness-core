/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.InfraDefinition;

import software.wings.beans.InfrastructureType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.CustomInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;

import java.util.List;

public class CustomInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, CustomInfrastructure> {
  @Override
  public Yaml toYaml(CustomInfrastructure bean, String appId) {
    return Yaml.builder()
        .type(InfrastructureType.CUSTOM_INFRASTRUCTURE)
        .infraVariables(bean.getInfraVariables())
        .deploymentTypeTemplateVersion(bean.getDeploymentTypeTemplateVersion())
        .build();
  }

  @Override
  public CustomInfrastructure upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    return CustomInfrastructure.builder()
        .infraVariables(yaml.getInfraVariables())
        .deploymentTypeTemplateVersion(yaml.getDeploymentTypeTemplateVersion())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
