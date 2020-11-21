package io.harness.delegate.task.azure.appservice.webapp.request;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSwapSlotsParameters extends AzureAppServiceTaskParameters {
  private String resourceGroupName;

  @Builder
  public AzureWebAppSwapSlotsParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String commandName, Integer timeoutIntervalInMin, AzureAppServiceTaskType commandType,
      AzureAppServiceType appServiceType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, commandType, appServiceType);
    this.resourceGroupName = resourceGroupName;
  }
}
