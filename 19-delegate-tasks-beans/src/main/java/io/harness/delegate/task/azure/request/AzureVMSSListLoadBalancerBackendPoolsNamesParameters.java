package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_LOAD_BALANCERS_NAMES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSListLoadBalancerBackendPoolsNamesParameters extends AzureVMSSTaskParameters {
  private String resourceGroupName;
  private String loadBalancerName;

  @Builder
  public AzureVMSSListLoadBalancerBackendPoolsNamesParameters(String appId, String accountId, String activityId,
      String commandName, String resourceGroupName, String loadBalancerName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_LIST_LOAD_BALANCERS_NAMES);
    this.resourceGroupName = resourceGroupName;
    this.loadBalancerName = loadBalancerName;
  }
}
