/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
