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
  void suspendChainExecution(Ambiance ambiance, SuspendChainRequest suspendChainRequest);

  void addExecutableResponse(Ambiance ambiance, ExecutableResponse executableResponse);

  default void handleStepResponse(Ambiance ambiance, @NonNull StepResponseProto stepResponse) {
    handleStepResponse(ambiance, stepResponse, null);
  }

  void handleStepResponse(
      Ambiance ambiance, @NonNull StepResponseProto stepResponse, ExecutableResponse executableResponse);

  void resumeNodeExecution(Ambiance ambiance, Map<String, ResponseData> response, boolean asyncError);

  @Deprecated
  void resumeNodeExecution(
      String planExecutionId, String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError);

  void handleFacilitationResponse(
      Ambiance ambiance, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto);

  void handleAdviserResponse(Ambiance ambiance, @NonNull String notifyId, AdviserResponse adviserResponse);

  void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo);

  void spawnChild(Ambiance ambiance, SpawnChildRequest spawnChildRequest);

  void queueTaskRequest(Ambiance ambiance, QueueTaskRequest queueTaskRequest);

  void spawnChildren(Ambiance ambiance, SpawnChildrenRequest spawnChildrenRequest);

  void handleProgressResponse(Ambiance ambiance, ProgressData progressData);
}
