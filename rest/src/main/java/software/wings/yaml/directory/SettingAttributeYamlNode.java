package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion.Type;

public class SettingAttributeYamlNode extends YamlNode {
  private String settingVariableType;

  public SettingAttributeYamlNode() {}

  public SettingAttributeYamlNode(String accountId, String uuid, String settingVariableType, String name,
      Class theClass, DirectoryPath directoryPath, YamlGitService yamlGitSyncService) {
    super(accountId, uuid, name, theClass, directoryPath, yamlGitSyncService, Type.SETTING);
    this.settingVariableType = settingVariableType;
  }

  public String getSettingVariableType() {
    return settingVariableType;
  }

  public void setType(String settingVariableType) {
    this.settingVariableType = settingVariableType;
  }
}
