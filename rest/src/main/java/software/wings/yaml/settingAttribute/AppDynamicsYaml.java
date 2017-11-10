package software.wings.yaml.settingAttribute;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;

public class AppDynamicsYaml extends VerificationProviderYaml {
  private String accountname;

  public AppDynamicsYaml() {
    super();
  }

  public AppDynamicsYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    this.setUrl(appDynamicsConfig.getControllerUrl());
    this.setUsername(appDynamicsConfig.getUsername());

    this.accountname = appDynamicsConfig.getAccountname();
  }

  public String getAccountname() {
    return accountname;
  }

  public void setAccountname(String accountname) {
    this.accountname = accountname;
  }
}