package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.LogzConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class LogzConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, LogzConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    LogzConfig config = (LogzConfig) settingAttribute.getValue();
    return new Yaml(
        config.getType(), settingAttribute.getName(), config.getLogzUrl(), getEncryptedValue(config, "token", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    LogzConfig config = new LogzConfig();
    config.setAccountId(accountId);
    config.setEncryptedToken(yaml.getToken());
    config.setToken(null);
    config.setLogzUrl(yaml.getLogzUrl());

    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
