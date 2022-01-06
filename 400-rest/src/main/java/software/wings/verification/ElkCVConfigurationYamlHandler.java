/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import io.harness.exception.WingsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration.ElkCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import java.util.List;

public class ElkCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    final ElkCVConfigurationYaml yaml = (ElkCVConfigurationYaml) super.toYaml(bean, appId);
    if (!(bean instanceof ElkCVConfiguration)) {
      throw new WingsException("Unexpected type of cluster configuration");
    }

    ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) bean;
    yaml.setIndex(elkCVConfiguration.getIndex());
    yaml.setHostnameField(elkCVConfiguration.getHostnameField());
    yaml.setMessageField(elkCVConfiguration.getMessageField());
    yaml.setTimestampField(elkCVConfiguration.getTimestampField());
    yaml.setTimestampFormat(elkCVConfiguration.getTimestampFormat());
    return yaml;
  }

  @Override
  public LogsCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = getAppId(changeContext);
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    final ElkCVConfiguration bean = ElkCVConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);

    ElkCVConfigurationYaml yaml = (ElkCVConfigurationYaml) changeContext.getYaml();
    bean.setIndex(yaml.getIndex());
    bean.setHostnameField(yaml.getHostnameField());
    bean.setMessageField(yaml.getMessageField());
    bean.setTimestampField(yaml.getTimestampField());
    bean.setTimestampFormat(yaml.getTimestampFormat());

    saveToDatabase(bean, previous, appId);

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return ElkCVConfigurationYaml.class;
  }
}
