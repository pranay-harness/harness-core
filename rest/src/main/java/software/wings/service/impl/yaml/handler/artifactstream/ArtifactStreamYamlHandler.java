package software.wings.service.impl.yaml.handler.artifactstream;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

/**
 * @author rktummala on 10/09/17
 */
public abstract class ArtifactStreamYamlHandler<Y extends ArtifactStream.Yaml, B extends ArtifactStream>
    extends BaseYamlHandler<Y, B> {
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject YamlSyncHelper yamlSyncHelper;

  protected String getSettingId(String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, settingName);
    Validator.notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute);
    return settingAttribute.getUuid();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    Validator.notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute);
    return settingAttribute.getName();
  }

  protected B getArtifactStream(String accountId, String yamlFilePath) {
    Validator.notNullCheck("Yaml file path is null", yamlFilePath);
    return (B) yamlSyncHelper.getArtifactStream(accountId, yamlFilePath);
  }

  @Override
  public B get(String accountId, String yamlFilePath) {
    return getArtifactStream(accountId, yamlFilePath);
  }
}
