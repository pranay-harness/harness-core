package software.wings.service.impl.yaml.handler.setting.loadbalancer;

import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElasticLoadBalancerConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.Utils;

import com.amazonaws.regions.Regions;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class ElasticLoadBalancerConfigYamlHandler
    extends LoadBalancerYamlHandler<ElasticLoadBalancerConfigYaml, ElasticLoadBalancerConfig> {
  @Override
  public ElasticLoadBalancerConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElasticLoadBalancerConfig config = (ElasticLoadBalancerConfig) settingAttribute.getValue();
    ElasticLoadBalancerConfigYaml yaml =
        ElasticLoadBalancerConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(config.getType())
            .region(config.getRegion().name())
            .loadBalancerName(config.getLoadBalancerName())
            .accessKey(config.getAccessKey())
            .secretKey(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedSecretKey()))
            .useEc2IamCredentials(config.isUseEc2IamCredentials())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous,
      ChangeContext<ElasticLoadBalancerConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    ElasticLoadBalancerConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    // Regions region = Regions.fromName(yaml.getRegion());
    Regions region = Utils.getEnumFromString(Regions.class, yaml.getRegion());
    ElasticLoadBalancerConfig config = ElasticLoadBalancerConfig.builder()
                                           .accountId(accountId)
                                           .accessKey(yaml.getAccessKey())
                                           .loadBalancerName(yaml.getLoadBalancerName())
                                           .region(region)
                                           .encryptedSecretKey(yaml.getSecretKey())
                                           .useEc2IamCredentials(yaml.isUseEc2IamCredentials())
                                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return ElasticLoadBalancerConfigYaml.class;
  }
}
