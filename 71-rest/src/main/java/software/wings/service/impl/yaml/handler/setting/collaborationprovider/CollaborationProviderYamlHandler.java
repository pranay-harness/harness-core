package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CollaborationProviderYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class CollaborationProviderYamlHandler<Y extends CollaborationProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Inject private YamlHelper yamlHelper;

  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlHelper.getCollaborationProvider(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, Category.CONNECTOR);
  }
}
