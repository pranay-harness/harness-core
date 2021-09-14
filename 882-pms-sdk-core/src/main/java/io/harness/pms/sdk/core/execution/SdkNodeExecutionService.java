/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.NonNull;

@OwnedBy(CDC)
public interface SdkNodeExecutionService {
  void suspendChainExecution(
      String planExecutionId, String currentNodeExecutionId, SuspendChainRequest suspendChainRequest);

  void addExecutableResponse(
      @NonNull String planExecutionId, @NonNull String nodeExecutionId, ExecutableResponse executableResponse);

  default void handleStepResponse(
      String planExecutionId, @NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    handleStepResponse(planExecutionId, nodeExecutionId, stepResponse, null);
  }

  void handleStepResponse(String planExecutionId, @NonNull String nodeExecutionId,
      @NonNull StepResponseProto stepResponse, ExecutableResponse executableResponse);

  void resumeNodeExecution(
      String planExecutionId, String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError);

  void handleFacilitationResponse(String planExecutionId, @NonNull String nodeExecutionId, @NonNull String notifyId,
      FacilitatorResponseProto facilitatorResponseProto);

  void handleAdviserResponse(String planExecutionId, @NonNull String nodeExecutionId, @NonNull String notifyId,
      AdviserResponse adviserResponse);

  void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo);

  void spawnChild(String planExecutionId, String nodeExecutionId, SpawnChildRequest spawnChildRequest);

  void queueTaskRequest(String planExecutionId, String nodeExecutionId, QueueTaskRequest queueTaskRequest);

  void spawnChildren(String planExecutionId, String nodeExecutionId, SpawnChildrenRequest spawnChildrenRequest);

  void handleProgressResponse(Ambiance ambiance, ProgressData progressData);
}
