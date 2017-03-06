package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Created by bzane on 2/28/17
 * TODO(brett): Implement
 */
@Singleton
public class GcpSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return settingsService.getSettingAttributesByType(appId, SettingVariableTypes.GCP.name())
        .stream()
        .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
