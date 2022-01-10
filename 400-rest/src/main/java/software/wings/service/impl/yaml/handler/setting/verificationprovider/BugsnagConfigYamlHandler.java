/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.BugsnagConfig;
import software.wings.beans.BugsnagConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class BugsnagConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, BugsnagConfig> {
  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    BugsnagConfig config = BugsnagConfig.builder()
                               .accountId(accountId)
                               .url(yaml.getUrl())
                               .encryptedAuthToken(getSecretIdFromName(accountId, yaml.getAuthToken()))
                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Yaml toYaml(SettingAttribute bean, String appId) {
    BugsnagConfig config = (BugsnagConfig) bean.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .url(config.getUrl())
                    .authToken(getSecretNameFromId(config.getAccountId(), config.getEncryptedAuthToken()))
                    .build();
    toYaml(yaml, bean, appId);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
