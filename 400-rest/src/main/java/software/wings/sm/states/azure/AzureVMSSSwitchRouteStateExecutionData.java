/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure;

import static io.harness.azure.model.AzureConstants.ACTIVITY_ID;
import static io.harness.azure.model.AzureConstants.NEW_VIRTUAL_MACHINE_SCALE_SET;
import static io.harness.azure.model.AzureConstants.OLD_VIRTUAL_MACHINE_SCALE_SET;
import static io.harness.azure.model.AzureConstants.PROD_BACKEND_POOL;
import static io.harness.azure.model.AzureConstants.STAGE_BACKEND_POOL;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

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
public class AzureVMSSSwitchRouteStateExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String newVirtualMachineScaleSetName;
  private String oldVirtualMachineScaleSetName;
  private String stageBackendPool;
  private String prodBackendPool;

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

    putNotNull(executionDetails, NEW_VIRTUAL_MACHINE_SCALE_SET,
        ExecutionDataValue.builder()
            .value(newVirtualMachineScaleSetName)
            .displayName(NEW_VIRTUAL_MACHINE_SCALE_SET)
            .build());
    putNotNull(executionDetails, OLD_VIRTUAL_MACHINE_SCALE_SET,
        ExecutionDataValue.builder()
            .value(oldVirtualMachineScaleSetName)
            .displayName(OLD_VIRTUAL_MACHINE_SCALE_SET)
            .build());
    putNotNull(executionDetails, STAGE_BACKEND_POOL,
        ExecutionDataValue.builder().value(stageBackendPool).displayName(STAGE_BACKEND_POOL).build());
    putNotNull(executionDetails, PROD_BACKEND_POOL,
        ExecutionDataValue.builder().value(prodBackendPool).displayName(PROD_BACKEND_POOL).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }

  @Override
  public AzureVMSSSetupExecutionSummary getStepExecutionSummary() {
    return AzureVMSSSetupExecutionSummary.builder().build();
  }
}
