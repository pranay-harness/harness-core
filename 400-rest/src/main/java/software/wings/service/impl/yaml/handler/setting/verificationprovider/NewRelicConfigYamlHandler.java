/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class NewRelicConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, NewRelicConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NewRelicConfig config = (NewRelicConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(config.getType())
                    .apiKey(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedApiKey()))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    notNullCheck("api key is null", yaml.getApiKey());
    String accountId = changeContext.getChange().getAccountId();

    NewRelicConfig config = NewRelicConfig.builder()
                                .accountId(accountId)
                                .newRelicUrl("https://api.newrelic.com")
                                .encryptedApiKey(yaml.getApiKey())
                                .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
