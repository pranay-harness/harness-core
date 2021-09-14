/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.YamlVersion.Type;

@OwnedBy(HarnessTeam.DX)
public class SettingAttributeYamlNode extends YamlNode {
  private String settingVariableType;

  public SettingAttributeYamlNode() {}

  public SettingAttributeYamlNode(String accountId, String uuid, String settingVariableType, String name,
      Class theClass, DirectoryPath directoryPath) {
    super(accountId, uuid, name, theClass, directoryPath, Type.SETTING);
    this.settingVariableType = settingVariableType;
  }

  public String getSettingVariableType() {
    return settingVariableType;
  }

  public void setType(String settingVariableType) {
    this.settingVariableType = settingVariableType;
  }
}
