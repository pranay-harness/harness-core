/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SftpConfig;
import software.wings.beans.SftpConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class SftpConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, SftpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SftpConfig sftpConfig = (SftpConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(sftpConfig.getType())
                    .url(sftpConfig.getSftpUrl())
                    .username(sftpConfig.getUsername())
                    .password(getEncryptedYamlRef(sftpConfig.getAccountId(), sftpConfig.getEncryptedPassword()))
                    .domain(sftpConfig.getDomain())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    SftpConfig.Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SftpConfig config = SftpConfig.builder()
                            .accountId(accountId)
                            .sftpUrl(yaml.getUrl())
                            .username(yaml.getUsername())
                            .encryptedPassword(yaml.getPassword())
                            .domain(yaml.getDomain())
                            .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
