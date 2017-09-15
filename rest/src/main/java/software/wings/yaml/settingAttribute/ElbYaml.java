package software.wings.yaml.settingAttribute;

import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class ElbYaml extends SettingAttributeYaml {
  @YamlSerialize private String loadBalancerName;
  @YamlSerialize private String accessKey;
  @YamlSerialize private String secretKey;

  public ElbYaml() {
    super();
  }

  public ElbYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    ElasticLoadBalancerConfig elbConfig = (ElasticLoadBalancerConfig) settingAttribute.getValue();
    this.loadBalancerName = elbConfig.getLoadBalancerName();
    this.accessKey = elbConfig.getAccessKey();
    this.secretKey = elbConfig.getSecretKey().toString();
  }
}