package software.wings.yaml.settingAttribute;

import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;

public class ElkYaml extends VerificationProviderYaml {
  public ElkYaml() {
    super();
  }

  public ElkYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    this.setUrl(elkConfig.getElkUrl());
    this.setUsername(elkConfig.getUsername());
    this.setPassword(elkConfig.getPassword().toString());
  }
}
