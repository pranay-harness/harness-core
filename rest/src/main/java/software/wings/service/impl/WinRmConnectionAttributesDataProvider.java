package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;

@Singleton
public class WinRmConnectionAttributesDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;
  @Inject private AppService appService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<SettingAttribute> settingAttributeList = settingsService.getFilteredGlobalSettingAttributesByType(accountId,
        SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name(), appId, params.get(EntityType.ENVIRONMENT.name()));
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
