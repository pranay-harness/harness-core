/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_DEPLOYMENT_SLOTS;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppListWebAppDeploymentSlotsParameters extends AzureAppServiceTaskParameters {
  @Builder
  public AzureWebAppListWebAppDeploymentSlotsParameters(String appId, String accountId, String activityId,
      String subscriptionId, String commandName, int timeoutIntervalInMin, String resourceGroupName,
      String appServiceType, String appName) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, appName, commandName, timeoutIntervalInMin,
        LIST_WEB_APP_DEPLOYMENT_SLOTS, AzureAppServiceType.valueOf(appServiceType));
  }
}
