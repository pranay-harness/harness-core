/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class ArtifactServerYamlHandler<Y extends ArtifactServerYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    YamlType yamlType = yamlHelper.getYamlTypeFromSettingAttributePath(yamlFilePath);
    if (yamlType == null || yamlType == YamlType.ARTIFACT_SERVER) {
      return yamlHelper.getArtifactServer(accountId, yamlFilePath);
    } else {
      return yamlHelper.getArtifactServerAtConnector(accountId, yamlFilePath);
    }
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, SettingCategory.CONNECTOR);
  }
}
