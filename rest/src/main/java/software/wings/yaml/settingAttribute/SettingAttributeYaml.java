package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.yaml.GenericYaml;
import software.wings.yaml.YamlSerialize;

public class SettingAttributeYaml extends GenericYaml {
  @YamlSerialize private String name;
  @YamlSerialize private String type;

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
