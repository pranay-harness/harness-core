package software.wings.service.impl.sumo;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/12/17.
 */
public class SumoSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return settingsService.getSettingAttributesByType(appId, SettingVariableTypes.SUMO.name())
        .stream()
        .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
