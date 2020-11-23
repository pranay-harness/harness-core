package io.harness.delegate.task.azure.appservice.webapp.request;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSlotResizeParameters extends AzureAppServiceTaskParameters {
  private String resourceGroupName;
  private String webAppName;
  private String slotName;
  private double trafficWeight;
  private boolean isRollback;
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppSlotResizeParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String commandName, Integer timeoutIntervalInMin, AzureAppServiceTaskType commandType,
      AzureAppServiceType appServiceType, double trafficWeight, AzureAppServicePreDeploymentData preDeploymentData,
      boolean isRollback, String webAppName, String slotName) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin, commandType, appServiceType);
    this.resourceGroupName = resourceGroupName;
    this.trafficWeight = trafficWeight;
    this.preDeploymentData = preDeploymentData;
    this.isRollback = isRollback;
    this.webAppName = webAppName;
    this.slotName = slotName;
  }
}
