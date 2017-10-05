package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.YamlVersion.Type;

public class ServiceLevelYamlNode extends AppLevelYamlNode {
  private String serviceId;

  public ServiceLevelYamlNode() {
    super();
  }

  public ServiceLevelYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public ServiceLevelYamlNode(String uuid, String appId, String serviceId, String name, Class theClass,
      DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService, Type yamlVersionType) {
    super(uuid, appId, name, theClass, directoryPath, yamlGitSyncService, yamlVersionType);
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
