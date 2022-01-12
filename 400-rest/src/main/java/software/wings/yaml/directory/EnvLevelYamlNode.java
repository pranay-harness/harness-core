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
public class EnvLevelYamlNode extends AppLevelYamlNode {
  private String envId;

  public EnvLevelYamlNode() {}

  public EnvLevelYamlNode(String accountId, String uuid, String appId, String envId, String name, Class theClass,
      DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, uuid, appId, name, theClass, directoryPath, yamlVersionType);
    this.envId = envId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }
}
