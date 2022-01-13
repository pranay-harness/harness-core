/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.ecs;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
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
public class EcsListenerUpdateStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private EcsCommandRequest ecsCommandRequest;
  private String commandName;
  private EcsListenerUpdateRequestConfigData ecsListenerUpdateRequestConfigData;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> execDetails = super.getExecutionDetails();
    putNotNull(execDetails, "commandName",
        ExecutionDataValue.builder().value(commandName).displayName("Command Name").build());
    putNotNull(execDetails, "ClusterName",
        ExecutionDataValue.builder()
            .value(ecsListenerUpdateRequestConfigData.getClusterName())
            .displayName("Cluster Name")
            .build());
    putNotNull(execDetails, "Region",
        ExecutionDataValue.builder()
            .value(ecsListenerUpdateRequestConfigData.getRegion())
            .displayName("Region")
            .build());
    putNotNull(execDetails, "ProdListenerArn",
        ExecutionDataValue.builder()
            .value(ecsListenerUpdateRequestConfigData.getProdListenerArn())
            .displayName("Prod Listener ARN")
            .build());
    putNotNull(execDetails, "StageListenerArn",
        ExecutionDataValue.builder()
            .value(ecsListenerUpdateRequestConfigData.getStageListenerArn())
            .displayName("Stage Listener ARN")
            .build());
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(
        execDetails, "activityId", ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return execDetails;
  }

  @Override
  public EcsListenerUpdateExecutionSummary getStepExecutionSummary() {
    return EcsListenerUpdateExecutionSummary.builder()
        .updateRequestConfigData(ecsListenerUpdateRequestConfigData)
        .build();
  }
}
