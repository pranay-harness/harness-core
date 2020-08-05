package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_DEPLOY;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
  private boolean rollback;
  private List<String> baseScalingPolicyJSONs;

  @Builder
  public AzureVMSSDeployTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer timeoutIntervalInMin, AzureVMSSTaskType commandType, boolean resizeNewFirst,
      String newVirtualMachineScaleSetName, String oldVirtualMachineScaleSetName, Integer newDesiredCount,
      Integer oldDesiredCount, Integer autoScalingSteadyStateVMSSTimeout, int minInstances, int maxInstances,
      int desiredInstances, boolean rollback, List<String> baseScalingPolicyJSONs) {
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
    this.rollback = rollback;
    this.baseScalingPolicyJSONs = baseScalingPolicyJSONs;
  }
}
