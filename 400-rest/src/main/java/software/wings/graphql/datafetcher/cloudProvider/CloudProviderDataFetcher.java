/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.populateCloudProvider;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLCloudProviderQueryParameters;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class CloudProviderDataFetcher
    extends AbstractObjectDataFetcher<QLCloudProvider, QLCloudProviderQueryParameters> {
  @Inject private SettingsService settingsService;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLCloudProvider fetch(QLCloudProviderQueryParameters qlQuery, String accountId) {
    SettingAttribute settingAttribute = null;

    if (qlQuery.getCloudProviderId() != null) {
      settingAttribute = settingsService.getByAccount(accountId, qlQuery.getCloudProviderId());
    } else if (qlQuery.getName() != null) {
      settingAttribute = settingsService.getByName(accountId, GLOBAL_APP_ID, qlQuery.getName());
    }

    if (settingAttribute == null || settingAttribute.getValue() == null
        || CLOUD_PROVIDER != settingAttribute.getCategory()) {
      throw new InvalidRequestException("Cloud Provider does not exist", WingsException.USER);
    }

    return populateCloudProvider(settingAttribute).build();
  }
}
