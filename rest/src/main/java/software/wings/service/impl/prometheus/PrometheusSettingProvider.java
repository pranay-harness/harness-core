package software.wings.service.impl.prometheus;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;

/**
 * Created by raghu on 08/28/17.
 */
@Singleton
public class PrometheusSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    List<SettingAttribute> settingAttributeList = settingsService.getFilteredSettingAttributesByType(
        appId, SettingVariableTypes.PROMETHEUS.name(), appId, params.get(EntityType.ENVIRONMENT.name()));
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
