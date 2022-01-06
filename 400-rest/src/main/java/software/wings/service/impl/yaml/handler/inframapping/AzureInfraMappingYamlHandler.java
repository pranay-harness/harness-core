/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class AzureInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, AzureInfrastructureMapping> {
  @Override
  public AzureInfrastructureMapping.Yaml toYaml(AzureInfrastructureMapping bean, String appId) {
    AzureInfrastructureMapping.Yaml yaml = AzureInfrastructureMapping.Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.AZURE_INFRA.name());
    yaml.setSubscriptionId(bean.getSubscriptionId());
    yaml.setResourceGroup(bean.getResourceGroup());
    yaml.setAzureTags(bean.getTags());
    return yaml;
  }

  @Override
  public AzureInfrastructureMapping upsertFromYaml(
      ChangeContext<AzureInfrastructureMapping.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AzureInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    AzureInfrastructureMapping current = new AzureInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AzureInfrastructureMapping previous =
        (AzureInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(AzureInfrastructureMapping bean, ChangeContext<AzureInfrastructureMapping.Yaml> changeContext,
      String appId, String envId, String computeProviderId, String serviceId) {
    AzureInfrastructureMapping.Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setSubscriptionId(yaml.getSubscriptionId());
    bean.setResourceGroup(yaml.getResourceGroup());
    bean.setTags(yaml.getAzureTags());
  }

  @Override
  public Class getYamlClass() {
    return AzureInfrastructureMapping.Yaml.class;
  }

  @Override
  public AzureInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (AzureInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }
}
