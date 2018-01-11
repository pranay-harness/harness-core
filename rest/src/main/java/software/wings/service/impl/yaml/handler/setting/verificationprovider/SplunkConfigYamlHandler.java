package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SplunkConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SplunkConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SplunkConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SplunkConfig config = (SplunkConfig) settingAttribute.getValue();
    return new Yaml(
        config.getType(), config.getSplunkUrl(), config.getUsername(), getEncryptedValue(config, "password", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedPassword;
    try {
      decryptedPassword = secretManager.decryptYamlRef(yaml.getPassword());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the password ref:" + yaml.getPassword());
    }

    SplunkConfig config = SplunkConfig.builder()
                              .accountId(accountId)
                              .splunkUrl(yaml.getSplunkUrl())
                              .password(decryptedPassword)
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
