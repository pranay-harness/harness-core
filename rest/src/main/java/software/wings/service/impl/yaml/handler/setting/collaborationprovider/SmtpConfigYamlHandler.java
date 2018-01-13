package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.mail.SmtpConfig.Yaml;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SmtpConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SmtpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(smtpConfig.getType())
        .host(smtpConfig.getHost())
        .port(smtpConfig.getPort())
        .fromAddress(smtpConfig.getFromAddress())
        .useSSL(smtpConfig.isUseSSL())
        .username(smtpConfig.getUsername())
        .password(getEncryptedValue(smtpConfig, "password", false))
        .build();
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

    SmtpConfig config = SmtpConfig.builder()
                            .accountId(accountId)
                            .host(yaml.getHost())
                            .port(yaml.getPort())
                            .password(decryptedPassword)
                            .encryptedPassword(yaml.getPassword())
                            .username(yaml.getUsername())
                            .fromAddress(yaml.getFromAddress())
                            .useSSL(yaml.isUseSSL())
                            .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
