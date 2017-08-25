package software.wings.yaml.directory;

public class ServiceCommandYamlNode extends YamlNode {
  private String appId;
  private String serviceId;

  public ServiceCommandYamlNode() {
    super();
  }

  public ServiceCommandYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public ServiceCommandYamlNode(String uuid, String appId, String serviceId, String name, Class theClass) {
    super(uuid, name, theClass);
    this.appId = appId;
    this.serviceId = serviceId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
