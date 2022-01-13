/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.mail.SmtpConfig.Yaml;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class SmtpConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SmtpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(smtpConfig.getType())
                    .host(smtpConfig.getHost())
                    .port(smtpConfig.getPort())
                    .fromAddress(smtpConfig.getFromAddress())
                    .useSSL(smtpConfig.isUseSSL())
                    .username(smtpConfig.getUsername())
                    .password(smtpConfig.getEncryptedPassword() != null
                            ? getEncryptedYamlRef(smtpConfig.getAccountId(), smtpConfig.getEncryptedPassword())
                            : null)
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SmtpConfig config = SmtpConfig.builder()
                            .accountId(accountId)
                            .host(yaml.getHost())
                            .port(yaml.getPort())
                            .encryptedPassword(yaml.getPassword())
                            .username(yaml.getUsername())
                            .fromAddress(yaml.getFromAddress())
                            .useSSL(yaml.isUseSSL())
                            .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
