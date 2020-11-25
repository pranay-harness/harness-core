package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppListWebAppDeploymentSlotNamesParameters extends AzureAppServiceTaskParameters {
  @Builder
  public AzureWebAppListWebAppDeploymentSlotNamesParameters(String appId, String accountId, String activityId,
      String subscriptionId, String commandName, int timeoutIntervalInMin, String resourceGroupName,
      String appServiceType, String appName) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, appName, commandName, timeoutIntervalInMin,
        LIST_WEB_APP_DEPLOYMENT_SLOT_NAMES, AzureAppServiceType.valueOf(appServiceType));
  }
}
