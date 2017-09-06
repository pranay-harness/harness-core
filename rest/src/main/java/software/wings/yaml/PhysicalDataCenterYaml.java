package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;

public class PhysicalDataCenterYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;

  public PhysicalDataCenterYaml() {}

  public PhysicalDataCenterYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}