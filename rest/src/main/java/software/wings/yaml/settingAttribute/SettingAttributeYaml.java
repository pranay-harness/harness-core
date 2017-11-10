package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.yaml.BaseYaml;

public class SettingAttributeYaml extends BaseYaml {
  private String name;
  private String type;

  public SettingAttributeYaml() {}

  public SettingAttributeYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
    this.type = settingAttribute.getValue().getType();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
