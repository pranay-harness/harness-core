package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.YamlVersion.Type;

public class AppLevelYamlNode extends YamlNode {
  private String appId;

  public AppLevelYamlNode() {
    super();
  }

  public AppLevelYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public AppLevelYamlNode(String uuid, String appId, String name, Class theClass, Type yamlVersionType) {
    super(uuid, name, theClass, yamlVersionType);
    this.appId = appId;
  }

  public AppLevelYamlNode(String uuid, String appId, String name, Class theClass, DirectoryPath directoryPath,
      YamlGitSyncService yamlGitSyncService, Type yamlVersionType) {
    super(uuid, name, theClass, directoryPath, yamlGitSyncService, yamlVersionType);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
