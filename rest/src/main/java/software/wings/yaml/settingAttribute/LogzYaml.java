package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.LogzConfig;
import software.wings.yaml.YamlSerialize;

public class LogzYaml extends SettingAttributeYaml {
  @YamlSerialize private String url;
  @YamlSerialize private String token = ENCRYPTED_VALUE_STR;

  public LogzYaml() {
    super();
  }

  public LogzYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    LogzConfig logzConfig = (LogzConfig) settingAttribute.getValue();
    this.url = logzConfig.getLogzUrl();
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}