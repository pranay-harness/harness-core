/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure.appservices;

import static io.harness.azure.model.AzureConstants.ACTIVITY_ID;

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
public class AzureAppServiceSlotSwapExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String infrastructureMappingId;
  private String resourceGroup;
  private String appServiceName;
  private String deploymentSlot;
  private String targetSlot;

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
    putNotNull(executionDetails, "resourceGroup",
        ExecutionDataValue.builder().displayName("Resource Group").value(resourceGroup).build());
    putNotNull(executionDetails, "appServiceName",
        ExecutionDataValue.builder().displayName("Web App Name").value(appServiceName).build());
    putNotNull(executionDetails, "deploymentSlot",
        ExecutionDataValue.builder().displayName("Deployment Slot").value(deploymentSlot).build());
    putNotNull(executionDetails, "targetSlot",
        ExecutionDataValue.builder().displayName("Target Swap Slot").value(targetSlot).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }

  @Override
  public AzureAppServiceSlotSwapExecutionSummary getStepExecutionSummary() {
    return AzureAppServiceSlotSwapExecutionSummary.builder().build();
  }
}
