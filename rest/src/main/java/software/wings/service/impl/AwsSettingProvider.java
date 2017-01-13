package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by anubhaw on 12/12/16.
 */
@Singleton
public class AwsSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return settingsService.getSettingAttributesByType(appId, SettingVariableTypes.AWS.name())
        .stream()
        .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
