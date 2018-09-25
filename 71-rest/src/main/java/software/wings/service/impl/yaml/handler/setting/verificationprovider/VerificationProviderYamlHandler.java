package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class VerificationProviderYamlHandler<Y extends VerificationProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlHelper.getVerificationProvider(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, Category.CONNECTOR);
  }
}
