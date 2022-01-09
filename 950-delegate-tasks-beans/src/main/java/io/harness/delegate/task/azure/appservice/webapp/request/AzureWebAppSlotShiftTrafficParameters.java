/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SHIFT_TRAFFIC;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSlotShiftTrafficParameters extends AzureAppServiceTaskParameters {
  private double trafficWeightInPercentage;
  private String deploymentSlot;
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppSlotShiftTrafficParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String webAppName, String deploymentSlot, double trafficWeightInPercentage,
      AzureAppServicePreDeploymentData preDeploymentData, String commandName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, webAppName, commandName,
        timeoutIntervalInMin, SLOT_SHIFT_TRAFFIC, WEB_APP);
    this.trafficWeightInPercentage = trafficWeightInPercentage;
    this.deploymentSlot = deploymentSlot;
    this.preDeploymentData = preDeploymentData;
  }
}
