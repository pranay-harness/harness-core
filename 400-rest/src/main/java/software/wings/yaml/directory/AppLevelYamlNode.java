package software.wings.yaml.directory;

import software.wings.yaml.YamlVersion.Type;

public class AppLevelYamlNode extends YamlNode {
  private String appId;

  public AppLevelYamlNode() {}

  public AppLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public AppLevelYamlNode(String accountId, String uuid, String appId, String name, Class theClass,
      DirectoryPath directoryPath, Type yamlVersionType) {
    super(accountId, uuid, name, theClass, directoryPath, yamlVersionType);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
