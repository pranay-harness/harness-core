/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SWITCH_ROUTE;

import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSSwitchRouteTaskParameters extends AzureVMSSTaskParameters {
  private String subscriptionId;
  private String resourceGroupName;
  private String oldVMSSName;
  private String newVMSSName;
  private boolean downscaleOldVMSS;
  boolean rollback;
  private AzureVMSSPreDeploymentData preDeploymentData;
  private AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail;

  @Builder
  public AzureVMSSSwitchRouteTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer autoScalingSteadyStateVMSSTimeout, AzureVMSSTaskType commandType, String oldVMSSName, String newVMSSName,
      boolean downscaleOldVMSS, boolean rollback, AzureVMSSPreDeploymentData preDeploymentData,
      AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail, String subscriptionId, String resourceGroupName) {
    super(appId, accountId, activityId, commandName, autoScalingSteadyStateVMSSTimeout, AZURE_VMSS_SWITCH_ROUTE);
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.oldVMSSName = oldVMSSName;
    this.newVMSSName = newVMSSName;
    this.downscaleOldVMSS = downscaleOldVMSS;
    this.rollback = rollback;
    this.preDeploymentData = preDeploymentData;
    this.azureLoadBalancerDetail = azureLoadBalancerDetail;
  }
}
