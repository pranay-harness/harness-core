package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ElkConfig;
import software.wings.beans.ElkConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class ElkConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, ElkConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElkConfig config = (ElkConfig) settingAttribute.getValue();
    String connectorType = Util.getStringFromEnum(config.getElkConnector());
    return new Yaml(config.getType(), settingAttribute.getName(), config.getElkUrl(), config.getUsername(),
        getEncryptedValue(config, "password", false), connectorType);
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    ElkConfig config = new ElkConfig();
    config.setAccountId(accountId);
    config.setElkUrl(yaml.getElkUrl());
    config.setEncryptedPassword(yaml.getPassword());
    config.setPassword(yaml.getPassword().toCharArray());
    config.setUsername(yaml.getUsername());
    ElkConnector elkConnector = Util.getEnumFromString(ElkConnector.class, yaml.getConnectorType());
    config.setElkConnector(elkConnector);

    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
