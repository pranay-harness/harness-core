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
public class YamlNode extends DirectoryNode {
  private String uuid;
  private Type yamlVersionType;

  public YamlNode() {
    this.setType(NodeType.YAML);
  }

  public YamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.YAML);
  }

  public YamlNode(String accountId, String uuid, String name, Class theClass, Type yamlVersionType) {
    super(accountId, name, theClass);
    this.setType(NodeType.YAML);
    this.uuid = uuid;
    this.yamlVersionType = yamlVersionType;
  }

  public YamlNode(
      String accountId, String uuid, String name, Class theClass, DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, name, theClass, directoryPath, NodeType.YAML);
    this.uuid = uuid;
    this.yamlVersionType = yamlVersionType;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Type getYamlVersionType() {
    return yamlVersionType;
  }

  public void setYamlVersionType(Type yamlVersionType) {
    this.yamlVersionType = yamlVersionType;
  }
}
