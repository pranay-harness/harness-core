package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * @author rktummala on 11/19/17
 */
public abstract class ArtifactServerYamlHandler<Y extends ArtifactServerYaml, B extends SettingValue>
    extends SettingValueYamlHandler<Y, B> {
  @Override
  public SettingAttribute get(String accountId, String yamlFilePath) {
    return yamlHelper.getArtifactServer(accountId, yamlFilePath);
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, Category.CONNECTOR);
  }
}
