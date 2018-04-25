package software.wings.service.impl.newrelic;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by raghu on 08/28/17.
 */
@Singleton
public class NewRelicSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return settingsService.getSettingAttributesByType(appId, SettingVariableTypes.NEW_RELIC.name())
        .stream()
        .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
