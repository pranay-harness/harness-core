package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface SettingsService extends OwnedByAccount {
  /**
   * List.
   *
   * @param req the req
   * @param appIdFromRequest
   * @param envIdFromRequest
   * @return the page response
   */
  PageResponse<SettingAttribute> list(
      PageRequest<SettingAttribute> req, String appIdFromRequest, String envIdFromRequest);

  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Create.class) SettingAttribute forceSave(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute, boolean pushToGit);

  SettingAttribute get(String appId, String varId);

  SettingAttribute get(String appId, String envId, String varId);

  SettingAttribute get(String varId);

  SettingAttribute getSettingAttributeByName(String accountId, String settingAttributeName);

  @ValidationGroups(Update.class) SettingAttribute update(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Update.class) SettingAttribute update(@Valid SettingAttribute settingAttribute, boolean pushToGit);

  void delete(String appId, String varId);

  void delete(String appId, String varId, boolean pushToGit, boolean syncFromGit);

  SettingAttribute getByName(String accountId, String appId, String attributeName);

  SettingAttribute getByName(String accountId, String appId, String envId, String attributeName);

  void createDefaultApplicationSettings(String appId, String accountId, boolean syncFromGit);

  void createDefaultAccountSettings(String accountId);

  List<SettingAttribute> getSettingAttributesByType(String appId, String type);

  List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String type, String currentAppId, String currentEnvId);

  List<SettingAttribute> getSettingAttributesByType(String appId, String envId, String type);

  List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String envId, String type, String currentAppId, String currentEnvId);

  List<SettingAttribute> getSettingAttributesByType(String accountId, String appId, String envId, String type);

  List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type);

  List<SettingAttribute> getFilteredGlobalSettingAttributesByType(
      String accountId, String type, String currentAppId, String currentEnvId);

  void deleteSettingAttributesByType(String accountId, String appId, String envId, String type);

  SettingValue getSettingValueById(String accountId, String id);

  ValidationResult validate(SettingAttribute settingAttribute);

  ValidationResult validate(String varId);

  void deleteByYamlGit(String appId, String settingAttributeId, boolean syncFromGit);
  Map<String, String> listAccountDefaults(String accountId);

  Map<String, String> listAppDefaults(String accountId, String appId);
}
