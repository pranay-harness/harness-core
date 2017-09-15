package software.wings.yaml.directory;

public class ServiceLevelYamlNode extends AppLevelYamlNode {
  private String serviceId;

  public ServiceLevelYamlNode() {
    super();
  }

  public ServiceLevelYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public ServiceLevelYamlNode(String uuid, String appId, String serviceId, String name, Class theClass) {
    super(uuid, appId, name, theClass);
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
