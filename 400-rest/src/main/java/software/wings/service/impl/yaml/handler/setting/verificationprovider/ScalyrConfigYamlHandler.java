/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.ScalyrConfig;
import software.wings.beans.ScalyrConfig.ScalyrYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * Created by rsingh on 2/12/18.
 */
public class ScalyrConfigYamlHandler extends VerificationProviderYamlHandler<ScalyrYaml, ScalyrConfig> {
  @Override
  public ScalyrYaml toYaml(SettingAttribute settingAttribute, String appId) {
    ScalyrConfig config = (ScalyrConfig) settingAttribute.getValue();

    ScalyrYaml yaml = ScalyrYaml.builder()
                          .harnessApiVersion(getHarnessApiVersion())
                          .type(config.getType())
                          .scalyrUrl(config.getUrl())
                          .apiToken(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedApiToken()))
                          .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<ScalyrYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    ScalyrYaml yaml = changeContext.getYaml();
    notNullCheck("apiToken key is null", yaml.getApiToken());
    String accountId = changeContext.getChange().getAccountId();

    ScalyrConfig config = ScalyrConfig.builder()
                              .accountId(accountId)
                              .url(yaml.getScalyrUrl())
                              .encryptedApiToken(yaml.getApiToken())
                              .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return ScalyrYaml.class;
  }
}
