package io.harness.delegate.task.azure.appservice.webapp.request;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSwapSlotsParameters extends AzureAppServiceTaskParameters {
  private String resourceGroup;
  private String webApp;
  private String deploymentSlot;
  private String targetSlot;
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppSwapSlotsParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String commandName, Integer timeoutIntervalInMin, AzureAppServiceTaskType commandType,
      AzureAppServiceType appServiceType, String webApp, String deploymentSlot, String targetSlot,
      AzureAppServicePreDeploymentData preDeploymentData) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, commandType, appServiceType);
    this.resourceGroup = resourceGroupName;
    this.webApp = webApp;
    this.deploymentSlot = deploymentSlot;
    this.targetSlot = targetSlot;
    this.preDeploymentData = preDeploymentData;
  }
}
