/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR_URL;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConnectorSampleDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createDockerConnector(String accountId) {
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_HUB_CONNECTOR)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                DockerConfig.builder().accountId(accountId).dockerRegistryUrl(HARNESS_DOCKER_HUB_CONNECTOR_URL).build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .withSample(true)
            .build();

    SettingAttribute existing = settingsService.fetchSettingAttributeByName(
        accountId, HARNESS_DOCKER_HUB_CONNECTOR, SettingVariableTypes.DOCKER);
    if (existing != null) {
      return existing;
    }
    return settingsService.forceSave(dockerSettingAttribute);
  }
}
