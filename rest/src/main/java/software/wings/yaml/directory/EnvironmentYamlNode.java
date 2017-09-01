package software.wings.yaml.directory;

public class EnvironmentYamlNode extends YamlNode {
  private String appId;

  public EnvironmentYamlNode() {
    super();
  }

  public EnvironmentYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public EnvironmentYamlNode(String uuid, String appId, String name, Class theClass) {
    super(uuid, name, theClass);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
