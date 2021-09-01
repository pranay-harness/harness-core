package software.wings.yaml.directory;

import software.wings.yaml.YamlVersion.Type;

public class ServiceLevelYamlNode extends AppLevelYamlNode {
  private String serviceId;

  public ServiceLevelYamlNode() {}

  public ServiceLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public ServiceLevelYamlNode(String accountId, String uuid, String appId, String serviceId, String name,
      Class theClass, DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, uuid, appId, name, theClass, directoryPath, yamlVersionType);
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
