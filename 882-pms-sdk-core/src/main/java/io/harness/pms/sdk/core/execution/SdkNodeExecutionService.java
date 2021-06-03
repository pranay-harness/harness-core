package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.*;
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
  void suspendChainExecution(String currentNodeExecutionId, SuspendChainRequest suspendChainRequest,
      SdkResponseEventMetadata responseEventMetadata);

  void addExecutableResponse(@NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse,
      List<String> callbackIds, SdkResponseEventMetadata metadata);

  void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse,
      SdkResponseEventMetadata sdkResponseEventMetadata);

  void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError,
      SdkResponseEventMetadata sdkResponseEventMetadata);

  StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution);

  void handleFacilitationResponse(@NonNull String nodeExecutionId, @NonNull String notifyId,
      FacilitatorResponseProto facilitatorResponseProto, SdkResponseEventMetadata sdkResponseEventMetadata);

  void handleAdviserResponse(@NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse,
      SdkResponseEventMetadata sdkResponseEventMetadata);

  void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo,
      SdkResponseEventMetadata sdkResponseEventMetadata);

  void spawnChild(SpawnChildRequest spawnChildRequest, SdkResponseEventMetadata sdkResponseEventMetadata);

  void queueTaskRequest(QueueTaskRequest queueTaskRequest, SdkResponseEventMetadata sdkResponseEventMetadata);

  void spawnChildren(SpawnChildrenRequest spawnChildrenRequest, SdkResponseEventMetadata sdkResponseEventMetadata);

  void handleProgressResponse(NodeExecutionProto nodeExecutionProto, ProgressData progressData);
}
