/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
