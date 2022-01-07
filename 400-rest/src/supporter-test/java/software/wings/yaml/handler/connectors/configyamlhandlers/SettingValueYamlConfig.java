/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.handler.connectors.configyamlhandlers;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingValueYamlConfig {
  private String name;
  private String yamlDirPath;
  private String invalidYamlContent;
  private SettingValueYamlHandler yamlHandler;
  private SettingAttribute settingAttributeSaved;
  private Class configclazz;
  private String updateMethodName;
  private String currentFieldValue;
  private Class yamlClass;
}
