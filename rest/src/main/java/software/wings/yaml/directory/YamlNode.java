package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.YamlVersion.Type;

public class YamlNode extends DirectoryNode {
  private String uuid;
  private Type yamlVersionType;

  public YamlNode() {
    super();
    this.setType(NodeType.YAML);
  }

  public YamlNode(String name, Class theClass) {
    super(name, theClass);
    this.setType(NodeType.YAML);
  }

  public YamlNode(String uuid, String name, Class theClass, Type yamlVersionType) {
    super(name, theClass);
    this.setType(NodeType.YAML);
    this.uuid = uuid;
    this.yamlVersionType = yamlVersionType;
  }

  public YamlNode(String uuid, String name, Class theClass, DirectoryPath directoryPath,
      YamlGitSyncService yamlGitSyncService, Type yamlVersionType) {
    super(name, theClass, directoryPath, yamlGitSyncService, NodeType.YAML);
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
