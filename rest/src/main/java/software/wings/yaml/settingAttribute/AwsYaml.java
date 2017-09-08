package software.wings.yaml.settingAttribute;

import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class AwsYaml extends SettingAttributeYaml {
  @YamlSerialize private String accessKey;
  @YamlSerialize private String secretKey;

  public AwsYaml() {
    super();
  }

  public AwsYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    this.accessKey = awsConfig.getAccessKey();
    this.secretKey = awsConfig.getSecretKey().toString();
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
}