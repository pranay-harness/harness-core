package software.wings.service.impl.splunk;

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
 * Created by peeyushaggarwal on 11/10/16.
 */
@Singleton
public class SplunkSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    List<SettingAttribute> settingAttributeList = settingsService.getFilteredSettingAttributesByType(
        appId, SettingVariableTypes.SPLUNK.name(), appId, params.get(EntityType.ENVIRONMENT.name()));
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
