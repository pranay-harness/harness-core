package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SumoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class SumoConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SumoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SumoConfig config = (SumoConfig) settingAttribute.getValue();
    return Yaml.builder()
        .type(config.getType())
        .harnessApiVersion(getHarnessApiVersion())
        .sumoUrl(config.getSumoUrl())
        .accessId(getEncryptedValue(config, "accessId", true))
        .accessKey(getEncryptedValue(config, "accessKey", true))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SumoConfig config = new SumoConfig();
    config.setAccountId(accountId);
    config.setSumoUrl(yaml.getSumoUrl());
    config.setEncryptedAccessId(yaml.getAccessId());
    config.setEncryptedAccessKey(yaml.getAccessKey());

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
