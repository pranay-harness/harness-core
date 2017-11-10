package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.beans.SettingAttribute;

public class VerificationProviderYaml extends SettingAttributeYaml {
  private String url;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

  public VerificationProviderYaml() {
    super();
  }

  public VerificationProviderYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}