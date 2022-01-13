/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
public class AzureAppServiceSlotShiftTrafficExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String infrastructureMappingId;
  private Integer appServiceSlotSetupTimeOut;
  private String appServiceName;
  private String deploySlotName;
  private String trafficWeight;

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
    putNotNull(executionDetails, "appServiceName",
        ExecutionDataValue.builder().displayName("Web App Name").value(appServiceName).build());
    putNotNull(executionDetails, "deploySlotName",
        ExecutionDataValue.builder().displayName("Deployment Slot").value(deploySlotName).build());
    putNotNull(executionDetails, "trafficWeight",
        ExecutionDataValue.builder().displayName("Traffic %").value(trafficWeight).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }

  @Override
  public AzureAppServiceSlotShiftTrafficExecutionSummary getStepExecutionSummary() {
    return AzureAppServiceSlotShiftTrafficExecutionSummary.builder().build();
  }
}
