package software.wings.service.impl;

import static java.util.stream.Collectors.groupingBy;
import static software.wings.beans.HostConnectionAttributes.AccessType;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class SSHKeyDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;
  @Inject private AppService appService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(
        accountId, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name());

    Map<AccessType, List<SettingAttribute>> settingAttributeByType = settingAttributes.stream().collect(
        groupingBy(sa -> ((HostConnectionAttributes) sa.getValue()).getAccessType()));

    return Stream.of(KEY)
        .map(settingAttributeByType::get)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(
            Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getName, (v1, v2) -> v1, LinkedHashMap::new));
  }
}
