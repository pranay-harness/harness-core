package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 5/17/16.
 */
public interface SettingsService extends OwnedByAccount {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req);

  /**
   * Save.
   *
   * @param settingAttribute the setting attribute
   * @return the setting attribute
   */
  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute);

  /**
   * Save.
   *
   * @param settingAttribute the setting attribute
   * @return the setting attribute
   */
  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute, boolean pushToGit);

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param varId the var id
   * @return the setting attribute
   */
  SettingAttribute get(String appId, String varId);

  /**
   * Get setting attribute.
   *
   * @param appId the app id
   * @param envId the env id
   * @param varId the var id
   * @return the setting attribute
   */
  SettingAttribute get(String appId, String envId, String varId);

  /**
   * Gets the.
   *
   * @param varId the var id
   * @return the setting attribute
   */
  SettingAttribute get(String varId);

  SettingAttribute getSettingAttributeByName(String accountId, String settingAttributeName);

  /**
   * Update.
   *
   * @param settingAttribute the setting attribute
   * @return the setting attribute
   */
  @ValidationGroups(Update.class) SettingAttribute update(@Valid SettingAttribute settingAttribute);

  /**
   * Update.
   *
   * @param settingAttribute the setting attribute
   * @return the setting attribute
   */
  @ValidationGroups(Update.class) SettingAttribute update(@Valid SettingAttribute settingAttribute, boolean pushToGit);

  /**
   * Delete.
   *
   * @param appId the app id
   * @param varId the var id
   */
  void delete(String appId, String varId);

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  void delete(String appId, String varId, boolean pushToGit);

  /**
   * Gets the by name.
   *
   * @param accountId     the account id
   * @param appId         the app id
   * @param attributeName the attribute name
   * @return the by name
   */
  SettingAttribute getByName(String accountId, String appId, String attributeName);

  /**
   * Gets by name.
   *
   * @param accountId     the account id
   * @param envId         the env id
   * @param attributeName the attribute name
   * @return the by name
   */
  SettingAttribute getByName(String accountId, String appId, String envId, String attributeName);

  /**
   * Create default application settings.
   *
   * @param appId     the app id
   * @param accountId the account id
   */
  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#createDefaultApplicationSettings(java.lang.String)
   */
  void createDefaultApplicationSettings(String appId, String accountId);

  /**
   * Create default account settings.
   *
   * @param accountId the account id
   */
  void createDefaultAccountSettings(String accountId);

  /**
   * Gets the setting attributes by type.
   *
   * @param appId the app id
   * @param type  the type
   * @return the setting attributes by type
   */
  List<SettingAttribute> getSettingAttributesByType(String appId, String type);

  /**
   * Gets setting attributes by type.
   *
   * @param appId the app id
   * @param envId the env id
   * @param type  the type
   * @return the setting attributes by type
   */
  List<SettingAttribute> getSettingAttributesByType(String appId, String envId, String type);

  /**
   * Gets setting attributes by type.
   *
   * @param accountId the account id
   * @param appId     the app id
   * @param envId     the env id
   * @param type      the type
   * @return the setting attributes by type
   */
  List<SettingAttribute> getSettingAttributesByType(String accountId, String appId, String envId, String type);

  /**
   * Gets the global setting attributes by type.
   *
   * @param accountId the account id
   * @param type      the type
   * @return the global setting attributes by type
   */
  List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type);

  /**
   *
   * @param accountId the account id
   * @param appId     the app id
   * @param envId     the env Id
   * @param type      the setting attribute type
   */
  void deleteSettingAttributesByType(String accountId, String appId, String envId, String type);

  SettingAttribute getGlobalSettingAttributesById(String accountId, String id);

  /**
   * Validate the passed SettingAttribute for correctness
   * @param settingAttribute The POJO to validate
   * @return true if passed POJO is valid, false otherwise
   */
  ValidationResult validate(SettingAttribute settingAttribute);

  /**
   * Validate the SettingAttribute stored in the DB for correctness
   * @param varId The Id of the stores SettingAttribute
   * @return true / false
   */
  ValidationResult validate(String varId);
}
