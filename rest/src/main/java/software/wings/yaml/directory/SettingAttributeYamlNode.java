package software.wings.yaml.directory;

public class SettingAttributeYamlNode extends YamlNode {
  private String settingVariableType;

  public SettingAttributeYamlNode() {
    super();
  }

  public SettingAttributeYamlNode(String uuid, String settingVariableType, String name, Class theClass) {
    super(uuid, name, theClass);
    this.settingVariableType = settingVariableType;
  }

  public String getSettingVariableType() {
    return settingVariableType;
  }

  public void setType(String settingVariableType) {
    this.settingVariableType = settingVariableType;
  }
}
