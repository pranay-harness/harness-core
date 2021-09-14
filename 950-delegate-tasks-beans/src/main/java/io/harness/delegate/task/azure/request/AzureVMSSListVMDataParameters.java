/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_VM_DATA;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSListVMDataParameters extends AzureVMSSTaskParameters {
  private String subscriptionId;
  private String resourceGroupName;
  private String vmssId;

  @Builder
  public AzureVMSSListVMDataParameters(String appId, String accountId, String activityId, String commandName,
      Integer timeoutIntervalInMin, String subscriptionId, String resourceGroupName, String vmssId) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_LIST_VM_DATA);
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.vmssId = vmssId;
  }
}
