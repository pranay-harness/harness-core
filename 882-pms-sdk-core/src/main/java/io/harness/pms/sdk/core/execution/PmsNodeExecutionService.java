package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskMode;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import java.util.List;
import java.util.Map;
import lombok.NonNull;

@OwnedBy(CDC)
public interface PmsNodeExecutionService {
  void queueNodeExecution(NodeExecutionProto nodeExecution);
  String queueTask(String nodeExecutionId, TaskMode mode, Map<String, String> setupAbstractions, Task task);
  void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds);
  void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse);
  void resumeNodeExecution(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError);
  Map<String, ResponseData> accumulateResponses(String planExecutionId, String notifyId);
  StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution);
}
