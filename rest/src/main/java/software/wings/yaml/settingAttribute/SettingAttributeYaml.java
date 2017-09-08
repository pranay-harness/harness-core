package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.yaml.GenericYaml;
import software.wings.yaml.YamlSerialize;

public class SettingAttributeYaml extends GenericYaml {
  @YamlSerialize private String name;

  public SettingAttributeYaml() {}

  public SettingAttributeYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
