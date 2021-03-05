package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.mail.SmtpConfigYaml;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class SmtpConfigYamlHandler extends CollaborationProviderYamlHandler<SmtpConfigYaml, SmtpConfig> {
  @Override
  public SmtpConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();

    SmtpConfigYaml yaml = SmtpConfigYaml.builder()
                              .harnessApiVersion(getHarnessApiVersion())
                              .type(smtpConfig.getType())
                              .host(smtpConfig.getHost())
                              .port(smtpConfig.getPort())
                              .fromAddress(smtpConfig.getFromAddress())
                              .useSSL(smtpConfig.isUseSSL())
                              .username(smtpConfig.getUsername())
                              .password(smtpConfig.getEncryptedPassword() != null ? getEncryptedYamlRef(
                                            smtpConfig.getAccountId(), smtpConfig.getEncryptedPassword())
                                                                                  : null)
                              .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<SmtpConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    SmtpConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SmtpConfig config = SmtpConfig.builder()
                            .accountId(accountId)
                            .host(yaml.getHost())
                            .port(yaml.getPort())
                            .encryptedPassword(yaml.getPassword())
                            .username(yaml.getUsername())
                            .fromAddress(yaml.getFromAddress())
                            .useSSL(yaml.isUseSSL())
                            .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return SmtpConfigYaml.class;
  }
}
