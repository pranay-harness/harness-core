package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_NAMES;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppListWebAppNamesParameters extends AzureAppServiceTaskParameters {
  private String resourceGroupName;

  @Builder
  public AzureWebAppListWebAppNamesParameters(String appId, String accountId, String activityId, String commandName,
      int timeoutIntervalInMin, String subscriptionId, String resourceGroupName, String appServiceType) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, LIST_WEB_APP_NAMES,
        AzureAppServiceType.valueOf(appServiceType));
    this.resourceGroupName = resourceGroupName;
  }
}
