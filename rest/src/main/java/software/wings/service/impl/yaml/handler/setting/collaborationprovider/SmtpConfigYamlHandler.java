package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.mail.SmtpConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SmtpConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SmtpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();
    return new Yaml(smtpConfig.getType(), settingAttribute.getName(), smtpConfig.getHost(), smtpConfig.getPort(),
        smtpConfig.getFromAddress(), smtpConfig.isUseSSL(), smtpConfig.getUsername(),
        getEncryptedValue(smtpConfig, "password", false));
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SmtpConfig config = SmtpConfig.builder()
                            .accountId(accountId)
                            .host(yaml.getHost())
                            .port(yaml.getPort())
                            .password(yaml.getPassword().toCharArray())
                            .encryptedPassword(yaml.getPassword())
                            .username(yaml.getUsername())
                            .fromAddress(yaml.getFromAddress())
                            .useSSL(yaml.isUseSSL())
                            .build();
    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
