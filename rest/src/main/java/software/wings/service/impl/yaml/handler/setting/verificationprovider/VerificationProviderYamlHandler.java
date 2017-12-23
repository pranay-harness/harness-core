package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class VerificationProviderYamlHandler<Y extends VerificationProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Inject private YamlHelper yamlHelper;

  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlHelper.getVerificationProvider(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    String name = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(accountId)
        .withAppId(GLOBAL_APP_ID)
        .withCategory(Category.CONNECTOR)
        .withEnvId(GLOBAL_ENV_ID)
        .withName(name)
        .withUuid(uuid)
        .withValue(config)
        .build();
  }
}
