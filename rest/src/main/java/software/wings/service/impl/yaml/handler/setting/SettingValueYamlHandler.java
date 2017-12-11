package software.wings.service.impl.yaml.handler.setting;

import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public abstract class SettingValueYamlHandler<Y extends SettingValue.Yaml, B extends SettingValue>
    extends BaseYamlHandler<Y, SettingAttribute> {
  private static final Logger logger = LoggerFactory.getLogger(SettingValueYamlHandler.class);

  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;

  @Override
  public SettingAttribute upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    SettingAttribute previous = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    SettingAttribute settingAttribute = setWithYamlValues(previous, changeContext, changeSetContext);
    if (previous != null) {
      return settingsService.update(settingAttribute);
    } else {
      return settingsService.save(settingAttribute);
    }
  }

  protected abstract SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    Y yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getType() == null);
  }

  protected String getEncryptedValue(Encryptable settingValue, String fieldName, boolean encryptMultipleFields)
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
  public SettingAttribute createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    throw new HarnessException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public SettingAttribute updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    throw new HarnessException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    SettingAttribute settingAttribute =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (settingAttribute != null) {
      settingsService.delete(GLOBAL_APP_ID, settingAttribute.getUuid());
    }
  }
}
