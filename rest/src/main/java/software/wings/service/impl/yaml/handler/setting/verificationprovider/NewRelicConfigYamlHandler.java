package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class NewRelicConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, NewRelicConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NewRelicConfig config = (NewRelicConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .apiKey(getEncryptedValue(config, "apiKey", false))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedApiKey;
    try {
      decryptedApiKey = secretManager.decryptYamlRef(yaml.getApiKey());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the api key ref:" + yaml.getApiKey());
    }

    NewRelicConfig config = NewRelicConfig.builder()
                                .accountId(accountId)
                                .newRelicUrl("https://api.newrelic.com")
                                .apiKey(decryptedApiKey)
                                .encryptedApiKey(yaml.getApiKey())
                                .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
