package software.wings.service.impl.apm;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;

import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;

public class APMSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    List<SettingAttribute> settingAttributeList = settingsService.getFilteredSettingAttributesByType(appId,
        SettingValue.SettingVariableTypes.APM_VERIFICATION.name(), appId, params.get(EntityType.ENVIRONMENT.name()));
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
