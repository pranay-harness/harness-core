package software.wings.yaml.directory;

import software.wings.yaml.YamlVersion.Type;

public class AccountLevelYamlNode extends YamlNode {
  public AccountLevelYamlNode() {}

  public AccountLevelYamlNode(
      String accountId, String uuid, String name, Class theClass, DirectoryPath directoryPath, Type type) {
    super(accountId, uuid, name, theClass, directoryPath, type);
  }
}
