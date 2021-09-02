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
