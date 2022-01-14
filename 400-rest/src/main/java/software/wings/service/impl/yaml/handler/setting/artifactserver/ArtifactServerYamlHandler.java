package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.yaml.YamlType;
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
    YamlType yamlType = yamlHelper.getYamlTypeFromSettingAttributePath(yamlFilePath);
    if (yamlType == null || yamlType == YamlType.ARTIFACT_SERVER) {
      return yamlHelper.getArtifactServer(accountId, yamlFilePath);
    } else {
      return yamlHelper.getArtifactServerAtConnector(accountId, yamlFilePath);
    }
  }

  protected SettingAttribute buildSettingAttribute(String accountId, String yamlFilePath, String uuid, B config) {
    return buildSettingAttribute(accountId, yamlFilePath, uuid, config, SettingCategory.CONNECTOR);
  }
}
