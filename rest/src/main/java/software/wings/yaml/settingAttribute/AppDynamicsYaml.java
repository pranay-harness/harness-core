package software.wings.yaml.settingAttribute;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class AppDynamicsYaml extends VerificationProviderYaml {
  @YamlSerialize private String accountname;

  public AppDynamicsYaml() {
    super();
  }

  public AppDynamicsYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    this.setUrl(appDynamicsConfig.getControllerUrl());
    this.setUsername(appDynamicsConfig.getUsername());
    this.setPassword(appDynamicsConfig.getPassword().toString());

    this.accountname = appDynamicsConfig.getAccountname();
  }

  public String getAccountname() {
    return accountname;
  }

  public void setAccountname(String accountname) {
    this.accountname = accountname;
  }
}