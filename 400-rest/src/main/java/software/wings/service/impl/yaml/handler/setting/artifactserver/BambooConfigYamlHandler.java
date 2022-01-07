/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.BambooConfig;
import software.wings.beans.BambooConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class BambooConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, BambooConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(bambooConfig.getType())
                    .url(bambooConfig.getBambooUrl())
                    .username(bambooConfig.getUsername())
                    .password(getEncryptedYamlRef(bambooConfig.getAccountId(), bambooConfig.getEncryptedPassword()))
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

    BambooConfig config = BambooConfig.builder()
                              .accountId(accountId)
                              .bambooUrl(yaml.getUrl())
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
