package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SumoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SumoConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SumoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SumoConfig config = (SumoConfig) settingAttribute.getValue();
    return new Yaml(config.getType(), config.getSumoUrl(), getEncryptedValue(config, "accessId", true),
        getEncryptedValue(config, "accessKey", true));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedAccessId;
    try {
      decryptedAccessId = secretManager.decryptYamlRef(yaml.getAccessId());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the access id ref:" + yaml.getAccessId());
    }

    char[] decryptedAccessKey;
    try {
      decryptedAccessKey = secretManager.decryptYamlRef(yaml.getAccessKey());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the access key ref:" + yaml.getAccessKey());
    }

    SumoConfig config = new SumoConfig();
    config.setAccountId(accountId);
    config.setSumoUrl(yaml.getSumoUrl());
    config.setAccessId(decryptedAccessId);
    config.setEncryptedAccessId(yaml.getAccessId());
    config.setAccessKey(decryptedAccessKey);
    config.setEncryptedAccessKey(yaml.getAccessKey());

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
