package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class NewRelicConfigYamlHandler extends VerificationProviderYamlHandler<NewRelicConfigYaml, NewRelicConfig> {
  @Override
  public NewRelicConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    NewRelicConfig config = (NewRelicConfig) settingAttribute.getValue();
    NewRelicConfigYaml yaml = NewRelicConfigYaml.builder()
                                  .harnessApiVersion(getHarnessApiVersion())
                                  .type(config.getType())
                                  .apiKey(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedApiKey()))
                                  .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<NewRelicConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    NewRelicConfigYaml yaml = changeContext.getYaml();
    notNullCheck("api key is null", yaml.getApiKey());
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
    return NewRelicConfigYaml.class;
  }
}
