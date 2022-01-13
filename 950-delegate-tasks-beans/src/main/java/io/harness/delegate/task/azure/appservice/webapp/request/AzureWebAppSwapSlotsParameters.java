/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SWAP;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSwapSlotsParameters extends AzureAppServiceTaskParameters {
  private String sourceSlotName;
  private String targetSlotName;
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppSwapSlotsParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String commandName, Integer timeoutIntervalInMin, String webAppName,
      String sourceSlotName, String targetSlotName, AzureAppServicePreDeploymentData preDeploymentData) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, webAppName, commandName,
        timeoutIntervalInMin, SLOT_SWAP, WEB_APP);
    this.sourceSlotName = sourceSlotName;
    this.targetSlotName = targetSlotName;
    this.preDeploymentData = preDeploymentData;
  }
}
