package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CloudProviderYaml;

import javax.inject.Inject;

/**
 * @author rktummala on 11/19/17
 */
public abstract class CloudProviderYamlHandler<Y extends CloudProviderYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Inject private YamlSyncHelper yamlSyncHelper;

  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getCloudProvider(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String name, String uuid, B config) {
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(accountId)
        .withAppId(GLOBAL_APP_ID)
        .withCategory(Category.CLOUD_PROVIDER)
        .withEnvId(GLOBAL_ENV_ID)
        .withName(name)
        .withUuid(uuid)
        .withValue(config)
        .build();
  }
}
