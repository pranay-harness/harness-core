package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_LIST_SUBSCRIPTIONS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSListSubscriptionsParameters extends AzureVMSSTaskParameters {
  @Builder
  public AzureVMSSListSubscriptionsParameters(
      String appId, String accountId, String activityId, String commandName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_LIST_SUBSCRIPTIONS);
  }
}
