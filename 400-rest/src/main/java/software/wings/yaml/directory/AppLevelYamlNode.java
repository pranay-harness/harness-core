/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.YamlVersion.Type;

@OwnedBy(HarnessTeam.DX)
public class AppLevelYamlNode extends YamlNode {
  private String appId;

  public AppLevelYamlNode() {}

  public AppLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public AppLevelYamlNode(String accountId, String uuid, String appId, String name, Class theClass,
      DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, uuid, name, theClass, directoryPath, yamlVersionType);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
