package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class NewRelicConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, NewRelicConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NewRelicConfig config = (NewRelicConfig) settingAttribute.getValue();
    return new Yaml(config.getType(), getEncryptedValue(config, "apiKey", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    NewRelicConfig config = NewRelicConfig.builder()
                                .accountId(accountId)
                                .newRelicUrl("https://api.newrelic.com")
                                .encryptedApiKey(yaml.getApiKey())
                                .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
