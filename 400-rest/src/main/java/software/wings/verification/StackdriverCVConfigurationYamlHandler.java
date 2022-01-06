/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import io.harness.exception.WingsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;
import software.wings.verification.log.StackdriverCVConfiguration;
import software.wings.verification.log.StackdriverCVConfiguration.StackdriverCVConfigurationYaml;

import java.util.List;

/**
 * Created by Pranjal on 06/04/2019
 */
public class StackdriverCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public StackdriverCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final StackdriverCVConfigurationYaml yaml = (StackdriverCVConfigurationYaml) super.toYaml(bean, appId);
    if (!(bean instanceof StackdriverCVConfiguration)) {
      throw new WingsException("Unexpected type of cluster configuration");
    }

    StackdriverCVConfiguration stackdriverCVConfiguration = (StackdriverCVConfiguration) bean;
    yaml.setQuery(stackdriverCVConfiguration.getQuery());
    yaml.setHostnameField(stackdriverCVConfiguration.getHostnameField());
    yaml.setMessageField(stackdriverCVConfiguration.getMessageField());
    yaml.setLogsConfiguration(stackdriverCVConfiguration.isLogsConfiguration());
    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = getAppId(changeContext);
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    final StackdriverCVConfiguration bean = StackdriverCVConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);

    StackdriverCVConfigurationYaml yaml = (StackdriverCVConfigurationYaml) changeContext.getYaml();
    bean.setQuery(yaml.getQuery());
    bean.setHostnameField(yaml.getHostnameField());
    bean.setMessageField(yaml.getMessageField());
    bean.setLogsConfiguration(yaml.isLogsConfiguration());

    saveToDatabase(bean, previous, appId);

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return StackdriverCVConfigurationYaml.class;
  }
}
