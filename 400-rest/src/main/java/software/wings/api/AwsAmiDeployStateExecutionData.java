/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.beans.InstanceUnitType;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/22/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class AwsAmiDeployStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String commandName;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;
  private Integer newVersion;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;
  private ResizeStrategy resizeStrategy;
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();
  private boolean rollback;

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
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    String requestedCount = instanceCount + " " + (instanceUnitType == InstanceUnitType.PERCENTAGE ? "%" : "");
    putNotNull(executionDetails, "requestedCount",
        ExecutionDataValue.builder().displayName("Desired Instance Requested").value(requestedCount).build());
    if (resizeStrategy != null) {
      putNotNull(executionDetails, "resizeStrategy",
          ExecutionDataValue.builder().displayName("Resize Strategy").value(resizeStrategy.getDisplayName()).build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    return AmiStepExecutionSummary.builder()
        .instanceCount(instanceCount)
        .instanceUnitType(instanceUnitType)
        .newInstanceData(newInstanceData)
        .oldInstanceData(oldInstanceData)
        .build();
  }
}
