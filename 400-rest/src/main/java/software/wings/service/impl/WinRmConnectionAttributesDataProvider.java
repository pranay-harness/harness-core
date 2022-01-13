/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;
import software.wings.stencils.DataProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
@OwnedBy(CDP)
public class WinRmConnectionAttributesDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;
  @Inject private AppService appService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<SettingAttribute> settingAttributeList = settingsService.getFilteredGlobalSettingAttributesByType(accountId,
        SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name(), appId, params.get(EntityType.ENVIRONMENT.name()));
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }
}
