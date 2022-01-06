/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static com.google.common.base.Joiner.on;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.StateExecutionData;

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
public class AwsAmiSwitchRoutesStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private List<String> targetArns;
  private List<String> classicLbs;
  private List<String> stageTargetArns;
  private List<String> stageClassicLbs;
  private String newAutoScalingGroupName;
  private String oldAutoScalingGroupName;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private String joinStrings(List<String> strings) {
    if (isEmpty(strings)) {
      return null;
    }
    return on(",").join(strings);
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    putNotNull(executionDetails, "Old AutoScaling Group",
        ExecutionDataValue.builder().displayName("Old AutoScaling Group").value(oldAutoScalingGroupName).build());
    putNotNull(executionDetails, "New AutoScaling Group",
        ExecutionDataValue.builder().displayName("New AutoScaling Group").value(newAutoScalingGroupName).build());
    putNotNull(executionDetails, "Primary Target Group Arns",
        ExecutionDataValue.builder().displayName("Primary Target Group Arns").value(joinStrings(targetArns)).build());
    putNotNull(executionDetails, "Primary Classic LBs",
        ExecutionDataValue.builder().displayName("Primary Classic LBs").value(joinStrings(classicLbs)).build());
    putNotNull(executionDetails, "Stage Target Group Arns",
        ExecutionDataValue.builder()
            .displayName("Stage Target Group Arns")
            .value(joinStrings(stageTargetArns))
            .build());
    putNotNull(executionDetails, "Stage Classic LBs",
        ExecutionDataValue.builder().displayName("Stage Classic LBs").value(joinStrings(stageClassicLbs)).build());
    return executionDetails;
  }
}
