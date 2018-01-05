package software.wings.service.impl.yaml.handler.setting.loadbalancer;

import com.amazonaws.regions.Regions;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElasticLoadBalancerConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class ElasticLoadBalancerConfigYamlHandler extends LoadBalancerYamlHandler<Yaml, ElasticLoadBalancerConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElasticLoadBalancerConfig config = (ElasticLoadBalancerConfig) settingAttribute.getValue();
    return new Yaml(config.getType(), config.getRegion().getName(), config.getLoadBalancerName(), config.getAccessKey(),
        getEncryptedValue(config, "secretKey", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    Regions region = Util.getEnumFromString(Regions.class, yaml.getRegion());
    ElasticLoadBalancerConfig config = ElasticLoadBalancerConfig.builder()
                                           .accountId(accountId)
                                           .accessKey(yaml.getAccessKey())
                                           .loadBalancerName(yaml.getLoadBalancerName())
                                           .region(region)
                                           .encryptedSecretKey(yaml.getSecretKey())
                                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
