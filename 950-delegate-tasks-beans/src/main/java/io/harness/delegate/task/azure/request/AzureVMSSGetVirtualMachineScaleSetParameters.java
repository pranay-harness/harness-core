/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSGetVirtualMachineScaleSetParameters extends AzureVMSSTaskParameters {
  private String subscriptionId;
  private String resourceGroupName;
  private String vmssName;

  @Builder
  public AzureVMSSGetVirtualMachineScaleSetParameters(String appId, String accountId, String activityId,
      String commandName, String subscriptionId, String vmssName, String resourceGroupName,
      Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_GET_VIRTUAL_MACHINE_SCALE_SET);
    this.subscriptionId = subscriptionId;
    this.vmssName = vmssName;
    this.resourceGroupName = resourceGroupName;
  }
}
