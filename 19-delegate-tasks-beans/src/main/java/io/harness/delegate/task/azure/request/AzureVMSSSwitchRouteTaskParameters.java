package io.harness.delegate.task.azure.request;

import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSSwitchRouteTaskParameters extends AzureVMSSTaskParameters {
  private String oldVMSSName;
  private String newVMSSName;
  private boolean downscaleOldVMSS;
  boolean rollback;
  private AzureVMSSPreDeploymentData preDeploymentData;
  private String baseScalingPolicyJSONs;
  private AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail;

  public AzureVMSSSwitchRouteTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer autoScalingSteadyStateVMSSTimeout, AzureVMSSTaskType commandType, String oldVMSSName, String newVMSSName,
      boolean downscaleOldVMSS, boolean rollback, AzureVMSSPreDeploymentData preDeploymentData,
      String baseScalingPolicyJSONs, AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail) {
    super(appId, accountId, activityId, commandName, autoScalingSteadyStateVMSSTimeout, commandType);
    this.oldVMSSName = oldVMSSName;
    this.newVMSSName = newVMSSName;
    this.downscaleOldVMSS = downscaleOldVMSS;
    this.rollback = rollback;
    this.preDeploymentData = preDeploymentData;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
    this.azureLoadBalancerDetail = azureLoadBalancerDetail;
  }
}
