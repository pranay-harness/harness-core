package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.SettingsService;
import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsSettingProvider implements DataProvider {
  @Inject private SettingsService settingsService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return settingsService.getSettingAttributesByType(appId, SettingVariableTypes.JENKINS)
        .stream()
        .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
