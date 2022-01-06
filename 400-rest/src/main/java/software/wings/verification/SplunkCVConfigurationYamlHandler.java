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
import software.wings.verification.log.SplunkCVConfiguration;
import software.wings.verification.log.SplunkCVConfiguration.SplunkCVConfigurationYaml;

import java.util.List;

public class SplunkCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final SplunkCVConfigurationYaml yaml = (SplunkCVConfigurationYaml) super.toYaml(bean, appId);
    if (!(bean instanceof SplunkCVConfiguration)) {
      throw new WingsException("Unexpected type of cluster configuration");
    }
    SplunkCVConfiguration splunkCVConfiguration = (SplunkCVConfiguration) bean;
    yaml.setAdvancedQuery(splunkCVConfiguration.isAdvancedQuery());
    yaml.setHostnameField(splunkCVConfiguration.getHostnameField());
    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    String appId = getAppId(changeContext);
    final SplunkCVConfiguration bean = SplunkCVConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);

    SplunkCVConfigurationYaml yaml = (SplunkCVConfigurationYaml) changeContext.getYaml();
    bean.setAdvancedQuery(yaml.isAdvancedQuery());
    bean.setHostnameField(yaml.getHostnameField());

    saveToDatabase(bean, previous, appId);

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return SplunkCVConfigurationYaml.class;
  }
}
