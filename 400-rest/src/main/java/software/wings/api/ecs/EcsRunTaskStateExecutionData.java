/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.ecs;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.GitFileConfig;
import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.EcsRunTaskDataBag;

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
public class EcsRunTaskStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String addTaskDefinition;
  private boolean skipSteadyStateCheck;
  private String ecsServiceName;
  String taskDefinitionJson;
  private String activityId;
  private String accountId;
  private String appId;
  private String commandName;
  private TaskType taskType;
  private GitFileConfig gitFileConfig;
  private EcsRunTaskDataBag ecsRunTaskDataBag;
  private Long serviceSteadyStateTimeout;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;

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
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(
        execDetails, "activityId", ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return execDetails;
  }

  @Override
  public EcsListenerUpdateExecutionSummary getStepExecutionSummary() {
    return EcsListenerUpdateExecutionSummary.builder().build();
  }
}
