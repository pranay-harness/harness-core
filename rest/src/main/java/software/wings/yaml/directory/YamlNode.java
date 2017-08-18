package software.wings.yaml.directory;

public class YamlNode extends DirectoryNode {
  public YamlNode() {
    super();
    this.setType("yaml");
  }

  public YamlNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("yaml");
  }
}
