/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
