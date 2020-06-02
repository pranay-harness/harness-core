package software.wings.service.impl.yaml.handler.setting.sourcerepoprovider;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.SourceRepoProviderYaml;

/**
 * @author dhruvupadhyay on 01/06/20
 */

public abstract class SourceRepoProviderYamlHandler<Y extends SourceRepoProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlHelper.getSourceRepoProvider(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, SettingAttribute.SettingCategory.CONNECTOR);
  }
}
