package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SplunkConfig;
import software.wings.beans.SplunkConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SplunkConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SplunkConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SplunkConfig config = (SplunkConfig) settingAttribute.getValue();
    return new Yaml(config.getType(), settingAttribute.getName(), config.getSplunkUrl(), config.getUsername(),
        getEncryptedValue(config, "password", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SplunkConfig config = SplunkConfig.builder()
                              .accountId(accountId)
                              .splunkUrl(yaml.getSplunkUrl())
                              .password(null)
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .build();
    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
