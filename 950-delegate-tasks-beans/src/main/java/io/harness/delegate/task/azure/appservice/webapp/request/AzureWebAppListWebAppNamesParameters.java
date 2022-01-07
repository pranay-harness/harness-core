/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_NAMES;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppListWebAppNamesParameters extends AzureAppServiceTaskParameters {
  @Builder
  public AzureWebAppListWebAppNamesParameters(String appId, String accountId, String activityId, String commandName,
      int timeoutIntervalInMin, String subscriptionId, String resourceGroupName, String appServiceType) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, null, commandName, timeoutIntervalInMin,
        LIST_WEB_APP_NAMES, AzureAppServiceType.valueOf(appServiceType));
  }
}
