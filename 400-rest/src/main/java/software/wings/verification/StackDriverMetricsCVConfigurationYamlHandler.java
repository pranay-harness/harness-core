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
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration.StackDriverMetricCVConfigurationYaml;

import java.util.List;

public class StackDriverMetricsCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<StackDriverMetricCVConfigurationYaml, StackDriverMetricCVConfiguration> {
  @Override
  public StackDriverMetricCVConfigurationYaml toYaml(StackDriverMetricCVConfiguration bean, String appId) {
    StackDriverMetricCVConfigurationYaml yaml = StackDriverMetricCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setMetricDefinitions(bean.getMetricDefinitions());

    yaml.setType(StateType.STACK_DRIVER.name());
    return yaml;
  }

  @Override
  public StackDriverMetricCVConfiguration upsertFromYaml(
      ChangeContext<StackDriverMetricCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    StackDriverMetricCVConfiguration bean = StackDriverMetricCVConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);

    bean.setMetricDefinitions(changeContext.getYaml().getMetricDefinitions());
    bean.setMetricFilters();
    bean.setStateType(StateType.STACK_DRIVER);

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
    return StackDriverMetricCVConfigurationYaml.class;
  }

  @Override
  public StackDriverMetricCVConfiguration get(String accountId, String yamlFilePath) {
    return (StackDriverMetricCVConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
}
