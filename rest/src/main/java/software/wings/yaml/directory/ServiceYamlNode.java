package software.wings.yaml.directory;

public class ServiceYamlNode extends YamlNode {
  private String appId;

  public ServiceYamlNode() {
    super();
  }

  public ServiceYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public ServiceYamlNode(String uuid, String appId, String name, Class theClass) {
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
