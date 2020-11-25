package io.harness.delegate.task.azure.appservice.webapp.request;

import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SHIFT_TRAFFIC;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;

import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppSlotShiftTrafficParameters extends AzureAppServiceTaskParameters {
  private String shiftTrafficSlotName;
  private double trafficWeightInPercentage;
  private boolean isRollback;
  private AzureAppServicePreDeploymentData preDeploymentData;

  @Builder
  public AzureWebAppSlotShiftTrafficParameters(String appId, String accountId, String activityId, String subscriptionId,
      String resourceGroupName, String webAppName, String shiftTrafficSlotName, double trafficWeightInPercentage,
      boolean isRollback, AzureAppServicePreDeploymentData preDeploymentData, String commandName,
      Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, subscriptionId, resourceGroupName, webAppName, commandName,
        timeoutIntervalInMin, SLOT_SHIFT_TRAFFIC, WEB_APP);
    this.shiftTrafficSlotName = shiftTrafficSlotName;
    this.trafficWeightInPercentage = trafficWeightInPercentage;
    this.isRollback = isRollback;
    this.preDeploymentData = preDeploymentData;
  }
}
