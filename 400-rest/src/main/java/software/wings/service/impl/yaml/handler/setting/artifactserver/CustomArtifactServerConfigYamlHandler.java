/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.CustomArtifactServerConfig;
import software.wings.beans.CustomArtifactServerConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class CustomArtifactServerConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, CustomArtifactServerConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    CustomArtifactServerConfig customArtifactServerConfig = (CustomArtifactServerConfig) settingAttribute.getValue();

    Yaml yaml =
        Yaml.builder().harnessApiVersion(getHarnessApiVersion()).type(customArtifactServerConfig.getType()).build();

    toYaml(yaml, settingAttribute, appId);

    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    String accountId = changeContext.getChange().getAccountId();
    CustomArtifactServerConfig customArtifactServerConfig =
        CustomArtifactServerConfig.builder().accountId(accountId).build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, customArtifactServerConfig);
  }
}
