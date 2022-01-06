/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.shell.AccessType.KERBEROS;
import static io.harness.shell.AccessType.KEY;
import static io.harness.shell.AccessType.USER_PASSWORD;

import static java.util.stream.Collectors.groupingBy;

import io.harness.annotations.dev.OwnedBy;
import io.harness.shell.AccessType;

import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@OwnedBy(CDP)
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

    return Stream.of(KEY, USER_PASSWORD, KERBEROS)
        .map(settingAttributeByType::get)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(
            Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getName, (v1, v2) -> v1, LinkedHashMap::new));
  }
}
