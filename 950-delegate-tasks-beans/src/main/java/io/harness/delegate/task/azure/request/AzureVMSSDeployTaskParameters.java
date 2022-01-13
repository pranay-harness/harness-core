/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_DEPLOY;

import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSDeployTaskParameters extends AzureVMSSTaskParameters {
  private boolean resizeNewFirst;
  private String newVirtualMachineScaleSetName;
  private String oldVirtualMachineScaleSetName;
  private Integer newDesiredCount;
  private Integer oldDesiredCount;
  private Integer autoScalingSteadyStateVMSSTimeout;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private String subscriptionId;
  private String resourceGroupName;
  private boolean rollback;
  private boolean isBlueGreen;
  private List<String> baseScalingPolicyJSONs;
  private AzureVMSSPreDeploymentData preDeploymentData;

  @Builder
  public AzureVMSSDeployTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer timeoutIntervalInMin, AzureVMSSTaskType commandType, boolean resizeNewFirst,
      String newVirtualMachineScaleSetName, String oldVirtualMachineScaleSetName, Integer newDesiredCount,
      Integer oldDesiredCount, Integer autoScalingSteadyStateVMSSTimeout, int minInstances, int maxInstances,
      int desiredInstances, boolean rollback, boolean isBlueGreen, List<String> baseScalingPolicyJSONs,
      String subscriptionId, String resourceGroupName, AzureVMSSPreDeploymentData preDeploymentData) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_DEPLOY);
    this.resizeNewFirst = resizeNewFirst;
    this.newVirtualMachineScaleSetName = newVirtualMachineScaleSetName;
    this.oldVirtualMachineScaleSetName = oldVirtualMachineScaleSetName;
    this.newDesiredCount = newDesiredCount;
    this.oldDesiredCount = oldDesiredCount;
    this.autoScalingSteadyStateVMSSTimeout = autoScalingSteadyStateVMSSTimeout;
    this.minInstances = minInstances;
    this.maxInstances = maxInstances;
    this.desiredInstances = desiredInstances;
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.rollback = rollback;
    this.isBlueGreen = isBlueGreen;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
    this.preDeploymentData = preDeploymentData;
  }
}
