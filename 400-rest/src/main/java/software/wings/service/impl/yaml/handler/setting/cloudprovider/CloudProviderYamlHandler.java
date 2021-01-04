package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CloudProviderYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class CloudProviderYamlHandler<Y extends CloudProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    YamlType yamlType = yamlHelper.getYamlTypeFromSettingAttributePath(yamlFilePath);
    if (yamlType == null || yamlType == YamlType.CLOUD_PROVIDER) {
      return yamlHelper.getCloudProvider(accountId, yamlFilePath);
    } else {
      return yamlHelper.getCloudProviderAtConnector(accountId, yamlFilePath);
    }
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, SettingCategory.CLOUD_PROVIDER);
  }
}
