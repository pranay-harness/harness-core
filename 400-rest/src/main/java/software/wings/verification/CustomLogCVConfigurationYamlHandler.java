/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.verification.log.CustomLogCVServiceConfiguration;
import software.wings.verification.log.CustomLogCVServiceConfiguration.CustomLogsCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomLogCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Override
  public CustomLogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    if (!(bean instanceof CustomLogCVServiceConfiguration)) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Unexpected type of CVConfiguration when trying to convert to yaml");
    }
    CustomLogCVServiceConfiguration customLogCVServiceConfiguration = (CustomLogCVServiceConfiguration) bean;
    CustomLogsCVConfigurationYaml yaml = (CustomLogsCVConfigurationYaml) super.toYaml(bean, appId);

    yaml.setLogCollectionInfo(customLogCVServiceConfiguration.getLogCollectionInfo());
    yaml.setType(StateType.LOG_VERIFICATION.name());
    return yaml;
  }

  @Override
  public CustomLogCVServiceConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = getAppId(changeContext);
    CVConfiguration previous = getPreviousCVConfiguration(changeContext);
    final CustomLogCVServiceConfiguration bean = CustomLogCVServiceConfiguration.builder().build();

    super.toBean(bean, changeContext, appId);
    CustomLogsCVConfigurationYaml yaml = (CustomLogsCVConfigurationYaml) changeContext.getYaml();
    bean.setLogCollectionInfo(yaml.getLogCollectionInfo());
    saveToDatabase(bean, previous, appId);
    return bean;
  }

  @Override
  public Class getYamlClass() {
    return CustomLogsCVConfigurationYaml.class;
  }
}
