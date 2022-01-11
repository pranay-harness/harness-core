/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.spotinst.model.ElastiGroup;

import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
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
@OwnedBy(CDP)
public class SpotInstSetupStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String elastiGroupId;
  private String elastiGroupName;
  private String serviceId;
  private String envId;
  private EnvironmentType environmentType;
  private String appId;
  private String infraMappingId;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private ElastiGroup elastiGroupOriginalConfig;
  private boolean rollback;

  private SpotInstCommandRequest spotinstCommandRequest;

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
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder()
            .value(spotinstCommandRequest.getSpotInstTaskParameters().getActivityId())
            .displayName("Activity Id")
            .build());
    putNotNull(executionDetails, "elastiGroupId",
        ExecutionDataValue.builder().value(elastiGroupId).displayName("Elasti Group ID").build());
    putNotNull(executionDetails, "elastiGroupName",
        ExecutionDataValue.builder().value(elastiGroupName).displayName("Elasti Group Name").build());

    return executionDetails;
  }

  @Override
  public SpotInstSetupExecutionSummary getStepExecutionSummary() {
    return SpotInstSetupExecutionSummary.builder().build();
  }
}
