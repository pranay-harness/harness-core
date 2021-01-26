package io.harness.delegate.task.azure.arm.request;

import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMDeploymentParameters extends AzureARMTaskParameters {
  private ARMScopeType deploymentScope;
  private AzureDeploymentMode deploymentMode;
  private String deploymentName;
  private String managementGroupId;
  private String subscriptionId;
  private String resourceGroupName;
  private String deploymentDataLocation;
  private String templateJson;
  private String parametersJson;

  @Builder
  public AzureARMDeploymentParameters(String appId, String accountId, String activityId, ARMScopeType deploymentScope,
      AzureDeploymentMode deploymentMode, String deploymentName, String managementGroupId, String subscriptionId,
      String resourceGroupName, String deploymentDataLocation, String templateJson, String parametersJson,
      String commandName, Integer timeoutIntervalInMin) {
    super(appId, accountId, activityId, subscriptionId, commandName, timeoutIntervalInMin,
        AzureARMTaskType.ARM_DEPLOYMENT);
    this.deploymentScope = deploymentScope;
    this.deploymentMode = deploymentMode;
    this.deploymentName = deploymentName;
    this.managementGroupId = managementGroupId;
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.deploymentDataLocation = deploymentDataLocation;
    this.templateJson = templateJson;
    this.parametersJson = parametersJson;
  }
}
