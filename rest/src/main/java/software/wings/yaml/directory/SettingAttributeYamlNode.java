package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.YamlVersion.Type;

public class SettingAttributeYamlNode extends YamlNode {
  private String settingVariableType;

  public SettingAttributeYamlNode() {
    super();
  }

  public SettingAttributeYamlNode(String uuid, String settingVariableType, String name, Class theClass,
      DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService) {
    super(uuid, name, theClass, directoryPath, yamlGitSyncService, Type.SETTING);
    this.settingVariableType = settingVariableType;
  }

  public String getSettingVariableType() {
    return settingVariableType;
  }

  public void setType(String settingVariableType) {
    this.settingVariableType = settingVariableType;
  }
}
