package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import java.util.List;
import java.util.Map;
import lombok.NonNull;

@OwnedBy(CDC)
public interface SdkNodeExecutionService {
  void suspendChainExecution(String currentNodeExecutionId, SuspendChainRequest suspendChainRequest);

  void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds);

  void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse);

  void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError);

  StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution);

  void handleFacilitationResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto);

  void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse);

  void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo);

  void spawnChild(SpawnChildRequest spawnChildRequest);

  void queueTaskRequest(QueueTaskRequest queueTaskRequest);

  void spawnChildren(SpawnChildrenRequest spawnChildrenRequest);

  void handleProgressResponse(Ambiance ambiance, ProgressData progressData);
}
