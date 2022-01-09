/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.spotinst.model.ElastiGroup;

import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
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
@OwnedBy(CDP)
public class SpotInstDeployStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String appId;
  private String infraId;
  private String envId;
  private String serviceId;
  private String activityId;
  private String newElastiGroupId;
  private String newElastiGroupName;
  private Integer newDesiredCount;

  private String oldElastiGroupId;
  private String oldElastiGroupName;
  private Integer oldDesiredCount;
  private String commandName;
  private ElastiGroup newElastiGroupOriginalConfig;
  private ElastiGroup oldElastiGroupOriginalConfig;

  private SpotInstCommandRequest spotinstCommandRequest;
  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder()
            .value(spotinstCommandRequest.getSpotInstTaskParameters().getActivityId())
            .displayName("Activity Id")
            .build());
    putNotNull(executionDetails, "newElastiGroupId",
        ExecutionDataValue.builder().value(newElastiGroupId).displayName("New ElastiGroup ID").build());
    putNotNull(executionDetails, "newElastiGroupName",
        ExecutionDataValue.builder().value(newElastiGroupName).displayName("New ElastiGroup Name").build());
    putNotNull(executionDetails, "newDesiredCount",
        ExecutionDataValue.builder().value(newDesiredCount).displayName("New Group Desired Count: ").build());

    putNotNull(executionDetails, "oldElastiGroupId",
        ExecutionDataValue.builder().value(oldElastiGroupId).displayName("Old ElastiGroup ID").build());
    putNotNull(executionDetails, "oldElastiGroupName",
        ExecutionDataValue.builder().value(oldElastiGroupName).displayName("Old ElastiGroup Name").build());
    putNotNull(executionDetails, "oldDesiredCount",
        ExecutionDataValue.builder().value(oldDesiredCount).displayName("Old Group Desired Count: ").build());

    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public SpotinstDeployExecutionSummary getStepExecutionSummary() {
    return SpotinstDeployExecutionSummary.builder()
        .oldElastigroupId(oldElastiGroupId)
        .oldElastigroupName(oldElastiGroupName)
        .newElastigroupId(newElastiGroupId)
        .newElastigroupName(newElastiGroupName)
        .build();
  }
}
