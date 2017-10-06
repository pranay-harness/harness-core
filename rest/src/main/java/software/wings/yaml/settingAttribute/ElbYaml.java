package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class ElbYaml extends SettingAttributeYaml {
  @YamlSerialize private String loadBalancerName;
  @YamlSerialize private String accessKey;
  @YamlSerialize private String secretKey = ENCRYPTED_VALUE_STR;

  public ElbYaml() {
    super();
  }

  public ElbYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    ElasticLoadBalancerConfig elbConfig = (ElasticLoadBalancerConfig) settingAttribute.getValue();
    this.loadBalancerName = elbConfig.getLoadBalancerName();
    this.accessKey = elbConfig.getAccessKey();
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
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