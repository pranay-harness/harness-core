package software.wings.service.intfc;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/17/16.
 */
public interface SettingsService {
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
   * @param envVar the env var
   * @return the setting attribute
   */
  SettingAttribute save(SettingAttribute envVar);

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param varId the var id
   * @return the setting attribute
   */
  SettingAttribute get(String appId, String varId);

  /**
   * Gets the.
   *
   * @param varId the var id
   * @return the setting attribute
   */
  SettingAttribute get(String varId);

  /**
   * Update.
   *
   * @param envVar the env var
   * @return the setting attribute
   */
  SettingAttribute update(SettingAttribute envVar);

  /**
   * Delete.
   *
   * @param appId the app id
   * @param varId the var id
   */
  void delete(String appId, String varId);

  /**
   * Gets the by name.
   *
   * @param appId         the app id
   * @param attributeName the attribute name
   * @return the by name
   */
  SettingAttribute getByName(String appId, String attributeName);

  /**
   * Creates the default settings.
   *
   * @param appId the app id
   */
  void createDefaultSettings(String appId);

  /**
   * Gets the setting attributes by type.
   *
   * @param appId the app id
   * @param type  the type
   * @return the setting attributes by type
   */
  List<SettingAttribute> getSettingAttributesByType(String appId, SettingVariableTypes type);

  /**
   * Gets the global setting attributes by type.
   *
   * @param type the type
   * @return the global setting attributes by type
   */
  List<SettingAttribute> getGlobalSettingAttributesByType(SettingVariableTypes type);
}
