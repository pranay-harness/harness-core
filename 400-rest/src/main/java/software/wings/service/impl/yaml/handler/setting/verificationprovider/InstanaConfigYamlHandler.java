/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.InstanaConfig;
import software.wings.beans.InstanaConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class InstanaConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, InstanaConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    InstanaConfig config = (InstanaConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .instanaUrl(config.getInstanaUrl())
                    .apiToken(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedApiToken()))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    InstanaConfig config = InstanaConfig.builder()
                               .accountId(accountId)
                               .instanaUrl(yaml.getInstanaUrl())
                               .encryptedApiToken(yaml.getApiToken())

                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
