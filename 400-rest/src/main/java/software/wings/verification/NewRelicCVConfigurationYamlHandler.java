/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration.NewRelicCVConfigurationYaml;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class NewRelicCVConfigurationYamlHandler
    extends MetricCVConfigurationYamlHandler<NewRelicCVConfigurationYaml, NewRelicCVServiceConfiguration> {
  @Inject NewRelicService newRelicService;

  @Override
  public NewRelicCVConfigurationYaml toYaml(NewRelicCVServiceConfiguration bean, String appId) {
    NewRelicCVConfigurationYaml yaml = NewRelicCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    NewRelicApplication newRelicApplication =
        newRelicService.resolveApplicationId(bean.getConnectorId(), bean.getApplicationId());
    Preconditions.checkNotNull(
        newRelicApplication, "Invalid NewRelic ApplicationID when converting to YAML: " + bean.getApplicationId());

    yaml.setNewRelicApplicationName(newRelicApplication.getName());
    yaml.setMetrics(bean.getMetrics());
    yaml.setType(StateType.NEW_RELIC.name());
    return yaml;
  }

  @Override
  public NewRelicCVServiceConfiguration upsertFromYaml(
      ChangeContext<NewRelicCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    String appId = getAppId(changeContext);
    NewRelicCVServiceConfiguration bean = NewRelicCVServiceConfiguration.builder().build();
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
    return NewRelicCVConfigurationYaml.class;
  }

  @Override
  public NewRelicCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (NewRelicCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(
      NewRelicCVServiceConfiguration bean, ChangeContext<NewRelicCVConfigurationYaml> changeContext, String appId) {
    NewRelicCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setMetrics(yaml.getMetrics() == null ? new ArrayList<>() : yaml.getMetrics());
    SettingAttribute connector = getConnector(yaml, accountId);

    NewRelicApplication newRelicApplication =
        newRelicService.resolveApplicationName(connector.getUuid(), yaml.getNewRelicApplicationName());
    Preconditions.checkNotNull(newRelicApplication,
        "Invalid NewRelic Application name when saving YAML: " + yaml.getNewRelicApplicationName());

    bean.setApplicationId(String.valueOf(newRelicApplication.getId()));
    bean.setStateType(StateType.NEW_RELIC);
  }
}
