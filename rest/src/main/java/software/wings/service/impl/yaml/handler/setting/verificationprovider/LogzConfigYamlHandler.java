package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.LogzConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class LogzConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, LogzConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    LogzConfig config = (LogzConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .logzUrl(config.getLogzUrl())
        .token(getEncryptedValue(config, "token", false))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    LogzConfig logzConfig =
        LogzConfig.builder().accountId(accountId).logzUrl(yaml.getLogzUrl()).encryptedToken(yaml.getToken()).build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, logzConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
