/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.ecs;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
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
@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class EcsRoute53WeightUpdateStateExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String newServiceName;
  private String newServiceDiscoveryServiceArn;
  private int newServiceWeight;
  private String oldServiceName;
  private String oldServiceDiscoveryServiceArn;
  private int oldServiceWeight;

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
    putNotNull(executionDetails, "New Service Name",
        ExecutionDataValue.builder().displayName("New Service Name").value(newServiceName).build());
    putNotNull(executionDetails, "New Service Weight",
        ExecutionDataValue.builder().displayName("New Service Weight").value(newServiceWeight).build());
    putNotNull(executionDetails, "New Service Discovery Service Arn",
        ExecutionDataValue.builder()
            .displayName("New Service Discovery Service Arn")
            .value(newServiceDiscoveryServiceArn)
            .build());
    putNotNull(executionDetails, "Old Service Name",
        ExecutionDataValue.builder().displayName("Old Service Name").value(oldServiceName).build());
    putNotNull(executionDetails, "Old Service Discovery Service Arn",
        ExecutionDataValue.builder()
            .displayName("Old Service Discovery Service Arn")
            .value(oldServiceDiscoveryServiceArn)
            .build());
    putNotNull(executionDetails, "Old Service Weight",
        ExecutionDataValue.builder().displayName("Old Service Weight").value(oldServiceWeight).build());
    return executionDetails;
  }
}
