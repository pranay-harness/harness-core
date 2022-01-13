/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.DatadogConfig;
import software.wings.beans.DatadogYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class DatadogConfigYamlHandler extends VerificationProviderYamlHandler<DatadogYaml, DatadogConfig> {
  @Override
  public DatadogYaml toYaml(SettingAttribute settingAttribute, String appId) {
    DatadogConfig config = (DatadogConfig) settingAttribute.getValue();

    DatadogYaml yaml =
        DatadogYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(config.getType())
            .url(config.getUrl())
            .apiKey(getSecretNameFromId(config.getAccountId(), config.getEncryptedApiKey()))
            .applicationKey(getSecretNameFromId(config.getAccountId(), config.getEncryptedApplicationKey()))
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<DatadogYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    DatadogYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    DatadogConfig datadogConfig = DatadogConfig.builder()
                                      .accountId(accountId)
                                      .url(yaml.getUrl())
                                      .encryptedApiKey(getSecretIdFromName(accountId, yaml.getApiKey()))
                                      .encryptedApplicationKey(getSecretIdFromName(accountId, yaml.getApplicationKey()))
                                      .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, datadogConfig);
  }

  @Override
  public Class getYamlClass() {
    return DatadogYaml.class;
  }
}
