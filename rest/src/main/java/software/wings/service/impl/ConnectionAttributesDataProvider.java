package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ConnectionAttributesDataProvider implements DataProvider {
  @javax.inject.Inject private SettingsService settingsService;
  @javax.inject.Inject private AppService appService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(
        accountId, SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name());

    return settingAttributes.stream().collect(Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
