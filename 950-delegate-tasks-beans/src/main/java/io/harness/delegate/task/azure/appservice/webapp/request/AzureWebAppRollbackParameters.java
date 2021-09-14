/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_ROLLBACK;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppRollbackParameters extends AzureAppServiceTaskParameters {
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppRollbackParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String appName, AzureAppServicePreDeploymentData preDeploymentData, String commandName,
      Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, appName, commandName, timeoutIntervalInMin,
        SLOT_ROLLBACK, WEB_APP);
    this.preDeploymentData = preDeploymentData;
  }
}
