package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.LogzConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class LogzConfigYamlHandler extends VerificationProviderYamlHandler<LogzConfigYaml, LogzConfig> {
  @Override
  public LogzConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    LogzConfig config = (LogzConfig) settingAttribute.getValue();

    LogzConfigYaml yaml = LogzConfigYaml.builder()
                              .harnessApiVersion(getHarnessApiVersion())
                              .type(config.getType())
                              .logzUrl(config.getLogzUrl())
                              .token(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedToken()))
                              .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<LogzConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    LogzConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    LogzConfig logzConfig =
        LogzConfig.builder().accountId(accountId).logzUrl(yaml.getLogzUrl()).encryptedToken(yaml.getToken()).build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, logzConfig);
  }

  @Override
  public Class getYamlClass() {
    return LogzConfigYaml.class;
  }
}
