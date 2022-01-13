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

import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class GcpKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, GcpKubernetesInfrastructureMapping> {
  @Override
  public Yaml toYaml(GcpKubernetesInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.GCP_KUBERNETES.name());
    yaml.setNamespace(bean.getNamespace());
    yaml.setCluster(bean.getClusterName());
    yaml.setReleaseName(bean.getReleaseName());
    return yaml;
  }

  @Override
  public GcpKubernetesInfrastructureMapping upsertFromYaml(
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

    GcpKubernetesInfrastructureMapping current = new GcpKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    GcpKubernetesInfrastructureMapping previous =
        (GcpKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(GcpKubernetesInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId,
      String envId, String computeProviderId, String serviceId) {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setNamespace(yaml.getNamespace());
    bean.setClusterName(yaml.getCluster());
    bean.setReleaseName(yaml.getReleaseName());
  }

  @Override
  public GcpKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (GcpKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
