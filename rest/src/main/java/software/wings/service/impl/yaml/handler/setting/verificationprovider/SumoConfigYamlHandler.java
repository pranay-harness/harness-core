package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SumoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SumoConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SumoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SumoConfig config = (SumoConfig) settingAttribute.getValue();
    return new Yaml(config.getType(), settingAttribute.getName(), config.getSumoUrl(),
        getEncryptedValue(config, "accessId", true), getEncryptedValue(config, "accessKey", true));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SumoConfig config = new SumoConfig();
    config.setAccountId(accountId);
    config.setSumoUrl(yaml.getSumoUrl());
    config.setAccessId(null);
    config.setEncryptedAccessId(yaml.getAccessId());
    config.setAccessKey(null);
    config.setEncryptedAccessKey(yaml.getAccessKey());

    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
