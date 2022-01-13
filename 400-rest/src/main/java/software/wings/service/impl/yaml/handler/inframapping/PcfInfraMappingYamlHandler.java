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

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class PcfInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, PcfInfrastructureMapping> {
  @Override
  public Yaml toYaml(PcfInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.PCF_PCF.name());
    yaml.setOrganization(bean.getOrganization());
    yaml.setSpace(bean.getSpace());
    yaml.setTempRouteMap(bean.getTempRouteMap());
    yaml.setRouteMaps(bean.getRouteMaps());
    return yaml;
  }

  @Override
  public PcfInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
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

    PcfInfrastructureMapping current = new PcfInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    PcfInfrastructureMapping previous =
        (PcfInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(PcfInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) {
    Yaml yaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setOrganization(yaml.getOrganization());
    bean.setSpace(yaml.getSpace());
    bean.setTempRouteMap(yaml.getTempRouteMap());
    bean.setRouteMaps(yaml.getRouteMaps());
  }

  @Override
  public PcfInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (PcfInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
