/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_INSTANCES_DATA;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppListWebAppInstancesParameters extends AzureAppServiceTaskParameters {
  private String slotName;
  @Builder
  public AzureWebAppListWebAppInstancesParameters(String appId, String accountId, String activityId,
      String subscriptionId, String commandName, int timeoutIntervalInMin, String resourceGroupName,
      AzureAppServiceType appServiceType, String appName, String slotName) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, appName, commandName, timeoutIntervalInMin,
        LIST_WEB_APP_INSTANCES_DATA, appServiceType);
    this.slotName = slotName;
  }
}
