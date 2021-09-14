/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
