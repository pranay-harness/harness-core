/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.LogzConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class LogzConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, LogzConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    LogzConfig config = (LogzConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .logzUrl(config.getLogzUrl())
                    .token(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedToken()))
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

    LogzConfig logzConfig =
        LogzConfig.builder().accountId(accountId).logzUrl(yaml.getLogzUrl()).encryptedToken(yaml.getToken()).build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, logzConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
