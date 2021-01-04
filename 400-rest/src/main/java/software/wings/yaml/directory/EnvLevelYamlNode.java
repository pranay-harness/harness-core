package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion.Type;

public class EnvLevelYamlNode extends AppLevelYamlNode {
  private String envId;

  public EnvLevelYamlNode() {}

  public EnvLevelYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public EnvLevelYamlNode(String accountId, String uuid, String appId, String envId, String name, Class theClass,
      DirectoryPath directoryPath, YamlGitService yamlGitSyncService, Type yamlVersionType) {
    super(accountId, uuid, appId, name, theClass, directoryPath, yamlGitSyncService, yamlVersionType);
    this.envId = envId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }
}
