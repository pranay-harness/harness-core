package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;

public class YamlNode extends DirectoryNode {
  private String uuid;

  public YamlNode() {
    super();
    this.setType("yaml");
  }

  public YamlNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("yaml");
  }

  public YamlNode(String uuid, String name, Class theClass) {
    super(name, theClass);
    this.setType("yaml");
    this.uuid = uuid;
  }

  public YamlNode(
      String uuid, String name, Class theClass, DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService) {
    super(name, theClass, directoryPath, yamlGitSyncService);
    this.setType("yaml");
    this.uuid = uuid;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}
