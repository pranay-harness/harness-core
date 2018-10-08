package software.wings.service.impl.yaml.handler.setting;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public abstract class SettingValueYamlHandler<Y extends SettingValue.Yaml, B extends SettingValue>
    extends BaseYamlHandler<Y, SettingAttribute> {
  private static final Logger logger = LoggerFactory.getLogger(SettingValueYamlHandler.class);

  @Inject protected SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject protected EncryptionService encryptionService;
  @Inject protected YamlHelper yamlHelper;

  @Override
  public SettingAttribute upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    SettingAttribute previous = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    SettingAttribute settingAttribute = toBean(previous, changeContext, changeSetContext);
    settingAttribute.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      settingAttribute.setUuid(previous.getUuid());
      return settingsService.update(settingAttribute);
    } else {
      return settingsService.save(settingAttribute);
    }
  }

  protected abstract SettingAttribute toBean(SettingAttribute previous, ChangeContext<Y> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException;

  protected String getEncryptedValue(EncryptableSetting settingValue, String fieldName, boolean encryptMultipleFields)
      throws WingsException {
    try {
      return encryptMultipleFields ? secretManager.getEncryptedYamlRef(settingValue, fieldName)
                                   : secretManager.getEncryptedYamlRef(settingValue);
    } catch (IllegalAccessException e) {
      logger.warn("Invalid " + fieldName + ". Should be a valid url to a secret");
      throw new WingsException(e);
    }
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    SettingAttribute settingAttribute =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (settingAttribute != null) {
      settingsService.deleteByYamlGit(
          GLOBAL_APP_ID, settingAttribute.getUuid(), changeContext.getChange().isSyncFromGit());
    }
  }

  protected SettingAttribute buildSettingAttribute(
      String accountId, String yamlFilePath, String uuid, B config, Category category) {
    String name = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(accountId)
        .withAppId(GLOBAL_APP_ID)
        .withCategory(category)
        .withEnvId(GLOBAL_ENV_ID)
        .withName(name)
        .withUuid(uuid)
        .withValue(config)
        .build();
  }
}
