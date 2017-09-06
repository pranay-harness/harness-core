package software.wings.yaml.directory;

public class CloudProviderYamlNode extends YamlNode {
  private String settingVariableType;

  public CloudProviderYamlNode() {
    super();
  }

  public CloudProviderYamlNode(String uuid, String settingVariableType, String name, Class theClass) {
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
