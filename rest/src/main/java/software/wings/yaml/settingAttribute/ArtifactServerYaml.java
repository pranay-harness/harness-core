package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class ArtifactServerYaml extends SettingAttributeYaml {
  @YamlSerialize private String url;
  @YamlSerialize private String username;
  @YamlSerialize private String password;

  public ArtifactServerYaml() {
    super();
  }

  public ArtifactServerYaml(SettingAttribute settingAttribute) {
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
