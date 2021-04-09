package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.tasks.ResponseData;

import java.util.List;
import java.util.Map;
import lombok.NonNull;

@OwnedBy(CDC)
public interface SdkNodeExecutionService {
  void queueNodeExecution(NodeExecutionProto nodeExecution);

  void queueNodeExecutionAndAddExecutableResponse(String currentNodeExecutionId,
      QueueNodeExecutionRequest queueNodeExecutionRequest, AddExecutableResponseRequest addExecutableResponseRequest);

  void addExecutableResponseAndResumeNode(String currentNodeExecutionId,
      AddExecutableResponseRequest addExecutableResponseRequest, ResumeNodeExecutionRequest resumeNodeExecutionRequest);

  void queueTaskAndAddExecutableResponse(
      QueueTaskRequest queueTaskRequest, AddExecutableResponseRequest addExecutableResponseRequest);

  void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds);

  void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse);

  void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError);

  Map<String, ResponseData> accumulateResponses(String planExecutionId, String notifyId);

  StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution);

  void handleFacilitationResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, FacilitatorResponseProto facilitatorResponseProto);

  void handleAdviserResponse(
      @NonNull String nodeExecutionId, @NonNull String notifyId, AdviserResponse adviserResponse);

  void handleEventError(NodeExecutionEventType eventType, String eventNotifyId, FailureInfo failureInfo);
}
