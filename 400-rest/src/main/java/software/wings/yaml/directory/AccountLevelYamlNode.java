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
public class AccountLevelYamlNode extends YamlNode {
  public AccountLevelYamlNode() {}

  public AccountLevelYamlNode(
      String accountId, String uuid, String name, Class theClass, DirectoryPath directoryPath, Type type) {
    super(accountId, uuid, name, theClass, directoryPath, type);
  }
}
