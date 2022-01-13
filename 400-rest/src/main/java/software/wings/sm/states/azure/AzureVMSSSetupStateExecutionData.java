/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure;

import static io.harness.azure.model.AzureConstants.ACTIVITY_ID;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AzureVMSSSetupStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String infrastructureMappingId;
  private String newVirtualMachineScaleSetName;
  private String oldVirtualMachineScaleSetName;
  private Integer newVersion;
  private Integer maxInstances;
  private Integer desiredInstances;
  private ResizeStrategy resizeStrategy;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "newVirtualMachineScaleSetName",
        ExecutionDataValue.builder().displayName("New VMSS Name").value(newVirtualMachineScaleSetName).build());
    putNotNull(executionDetails, "maxInstances",
        ExecutionDataValue.builder().displayName("Max Instances").value(maxInstances).build());
    putNotNull(executionDetails, "desiredInstances",
        ExecutionDataValue.builder().displayName("Desired Instances").value(desiredInstances).build());

    putNotNull(executionDetails, "oldVirtualMachineScaleSetName",
        ExecutionDataValue.builder().displayName("Old VMSS Name").value(oldVirtualMachineScaleSetName).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }

  @Override
  public AzureVMSSSetupExecutionSummary getStepExecutionSummary() {
    return AzureVMSSSetupExecutionSummary.builder().build();
  }
}
