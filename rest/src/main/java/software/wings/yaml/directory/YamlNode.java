package software.wings.yaml.directory;

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

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}
