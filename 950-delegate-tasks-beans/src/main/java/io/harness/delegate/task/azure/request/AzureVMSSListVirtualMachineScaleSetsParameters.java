package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSListVirtualMachineScaleSetsParameters extends AzureVMSSTaskParameters {
  private String subscriptionId;
  private String resourceGroupName;

  @Builder
  public AzureVMSSListVirtualMachineScaleSetsParameters(String appId, String accountId, String activityId,
      String commandName, String subscriptionId, String resourceGroupName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_LIST_VIRTUAL_MACHINE_SCALE_SETS);
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
  }
}
