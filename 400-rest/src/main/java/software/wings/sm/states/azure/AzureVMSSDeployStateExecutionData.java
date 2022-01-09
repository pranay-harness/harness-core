/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure;

import static io.harness.azure.model.AzureConstants.ACTIVITY_ID;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
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
public class AzureVMSSDeployStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String infraMappingId;
  private String newVirtualMachineScaleSetId;
  private String newVirtualMachineScaleSetName;
  private Integer newDesiredCount;

  private String oldVirtualMachineScaleSetId;
  private String oldVirtualMachineScaleSetName;
  private Integer oldDesiredCount;
  private String commandName;

  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());

    putNotNull(executionDetails, "newVirtualMachineScaleSetId",
        ExecutionDataValue.builder().value(newVirtualMachineScaleSetId).displayName("New VMSS ID").build());
    putNotNull(executionDetails, "nameVirtualMachineScaleSetName",
        ExecutionDataValue.builder().value(newVirtualMachineScaleSetName).displayName("New VMSS Name").build());
    putNotNull(executionDetails, "newDesiredCount",
        ExecutionDataValue.builder().value(newDesiredCount).displayName("New VMSS Desired Count: ").build());

    putNotNull(executionDetails, "oldVirtualMachineScaleSetId",
        ExecutionDataValue.builder().value(oldVirtualMachineScaleSetId).displayName("Old VMSS ID").build());
    putNotNull(executionDetails, "oldVirtualMachineScaleSetName",
        ExecutionDataValue.builder().value(oldVirtualMachineScaleSetName).displayName("Old VMSS Name").build());
    putNotNull(executionDetails, "oldDesiredCount",
        ExecutionDataValue.builder().value(oldDesiredCount).displayName("Old VMSS Desired Count: ").build());

    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public AzureVMSSDeployExecutionSummary getStepExecutionSummary() {
    return AzureVMSSDeployExecutionSummary.builder()
        .oldVirtualMachineScaleSetId(oldVirtualMachineScaleSetId)
        .oldVirtualMachineScaleSetName(oldVirtualMachineScaleSetName)
        .newVirtualMachineScaleSetId(newVirtualMachineScaleSetId)
        .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
        .build();
  }
}
