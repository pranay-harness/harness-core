/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.verification.instana.InstanaCVConfiguration;
import software.wings.verification.instana.InstanaCVConfiguration.InstanaCVConfigurationYaml;

import java.util.List;

public class InstanaCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<InstanaCVConfigurationYaml, InstanaCVConfiguration> {
  @Override
  public InstanaCVConfigurationYaml toYaml(InstanaCVConfiguration bean, String appId) {
    InstanaCVConfigurationYaml yaml = InstanaCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);

    yaml.setType(StateType.INSTANA.name());

    yaml.setTagFilters(bean.getTagFilters());

    return yaml;
  }

  @Override
  public InstanaCVConfiguration upsertFromYaml(
      ChangeContext<InstanaCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    InstanaCVConfiguration bean = InstanaCVConfiguration.builder().build();
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return InstanaCVConfigurationYaml.class;
  }

  @Override
  public InstanaCVConfiguration get(String accountId, String yamlFilePath) {
    return (InstanaCVConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(
      InstanaCVConfiguration bean, ChangeContext<InstanaCVConfigurationYaml> changeContext, String appId) {
    InstanaCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setTagFilters(yaml.getTagFilters());
    bean.setStateType(StateType.INSTANA);
  }
}
