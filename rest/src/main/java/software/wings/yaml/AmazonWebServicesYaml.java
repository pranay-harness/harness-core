package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;

public class AmazonWebServicesYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String accessKey;
  @YamlSerialize public String secretKey;

  public AmazonWebServicesYaml() {}

  public AmazonWebServicesYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
    this.accessKey = ((AwsConfig) settingAttribute.getValue()).getAccessKey();
    this.secretKey = ((AwsConfig) settingAttribute.getValue()).getSecretKey().toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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