/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.loadbalancer;

import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElasticLoadBalancerConfig.Yaml;
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
public class ElasticLoadBalancerConfigYamlHandler extends LoadBalancerYamlHandler<Yaml, ElasticLoadBalancerConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElasticLoadBalancerConfig config = (ElasticLoadBalancerConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
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
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
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
    return Yaml.class;
  }
}
