package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion.Type;

public class AppLevelYamlNode extends YamlNode {
  private String appId;

  public AppLevelYamlNode() {}

  public AppLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public AppLevelYamlNode(
      String accountId, String uuid, String appId, String name, Class theClass, Type yamlVersionType) {
    super(accountId, uuid, name, theClass, yamlVersionType);
    this.appId = appId;
  }

  public AppLevelYamlNode(String accountId, String uuid, String appId, String name, Class theClass,
      DirectoryPath directoryPath, YamlGitService yamlGitSyncService, Type yamlVersionType) {
    super(accountId, uuid, name, theClass, directoryPath, yamlGitSyncService, yamlVersionType);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
