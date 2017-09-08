package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;

public class SplunkYaml extends VerificationProviderYaml {
  public SplunkYaml() {
    super();
  }

  public SplunkYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();
    this.setUrl(splunkConfig.getSplunkUrl());
    this.setUsername(splunkConfig.getUsername());
    this.setPassword(splunkConfig.getPassword().toString());
  }
}