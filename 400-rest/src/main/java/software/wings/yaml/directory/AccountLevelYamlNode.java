package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion.Type;

public class AccountLevelYamlNode extends YamlNode {
  public AccountLevelYamlNode() {}

  public AccountLevelYamlNode(String accountId, String uuid, String name, Class theClass, DirectoryPath directoryPath,
      YamlGitService yamlGitSyncService, Type type) {
    super(accountId, uuid, name, theClass, directoryPath, yamlGitSyncService, type);
  }
}
