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

import static software.wings.beans.RancherKubernetesInfrastructureMapping.Yaml;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.HarnessException;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class RancherKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, RancherKubernetesInfrastructureMapping> {
  @Override
  public Yaml toYaml(RancherKubernetesInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.RANCHER_KUBERNETES.name());
    yaml.setNamespace(bean.getNamespace());
    yaml.setReleaseName(bean.getReleaseName());
    return yaml;
  }

  @Override
  public RancherKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    RancherKubernetesInfrastructureMapping current = new RancherKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    RancherKubernetesInfrastructureMapping previous =
        (RancherKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(RancherKubernetesInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId,
      String envId, String computeProviderId, String serviceId) {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, serviceId, null);
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setNamespace(infraMappingYaml.getNamespace());
    bean.setReleaseName(infraMappingYaml.getReleaseName());

    // Hardcoding it to some value since its a not null field in db. This field was used in name generation logic, but
    // no more.
    bean.setClusterName("clusterName");
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public RancherKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (RancherKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }
}
