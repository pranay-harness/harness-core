/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.pcf.CfServiceData;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

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
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PcfDeployStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String releaseName;
  private String commandName;
  private List<CfServiceData> instanceData;
  private String updateDetails;
  private SetupSweepingOutputPcf setupSweepingOutputPcf;
  private List<InstanceStatusSummary> newInstanceStatusSummaries;

  private Integer updateCount;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "commandName",
        ExecutionDataValue.builder().value(commandName).displayName("CommandName").build());
    putNotNull(executionDetails, "updateDetails",
        ExecutionDataValue.builder().value(updateDetails).displayName("Resize Details").build());

    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return PcfDeployExecutionSummary.builder().releaseName(releaseName).instaceData(instanceData).build();
  }
}
