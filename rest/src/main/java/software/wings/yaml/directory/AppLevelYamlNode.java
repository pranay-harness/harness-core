package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;

public class AppLevelYamlNode extends YamlNode {
  private String appId;

  public AppLevelYamlNode() {
    super();
  }

  public AppLevelYamlNode(String name, Class theClass) {
    super(name, theClass);
  }

  public AppLevelYamlNode(String uuid, String appId, String name, Class theClass) {
    super(uuid, name, theClass);
    this.appId = appId;
  }

  public AppLevelYamlNode(String uuid, String appId, String name, Class theClass, DirectoryPath directoryPath,
      YamlGitSyncService yamlGitSyncService) {
    super(uuid, name, theClass, directoryPath, yamlGitSyncService);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
