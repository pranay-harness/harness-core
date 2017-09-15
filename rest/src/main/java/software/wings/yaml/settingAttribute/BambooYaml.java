package software.wings.yaml.settingAttribute;

import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;

public class BambooYaml extends ArtifactServerYaml {
  public BambooYaml() {
    super();
  }

  public BambooYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();
    this.setUrl(bambooConfig.getBambooUrl());
    this.setUsername(bambooConfig.getUsername());
    this.setPassword(bambooConfig.getPassword().toString());
  }
}