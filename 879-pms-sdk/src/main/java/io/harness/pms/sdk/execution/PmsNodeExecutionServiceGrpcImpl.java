package io.harness.pms.sdk.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskMode;
import io.harness.pms.contracts.plan.AddExecutableResponseRequest;
import io.harness.pms.contracts.plan.HandleStepResponseRequest;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.plan.QueueNodeExecutionRequest;
import io.harness.pms.contracts.plan.QueueTaskRequest;
import io.harness.pms.contracts.plan.QueueTaskResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Task;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class PmsNodeExecutionServiceGrpcImpl implements PmsNodeExecutionService {
  @Inject private NodeExecutionProtoServiceBlockingStub nodeExecutionProtoServiceBlockingStub;
  @Inject private StepRegistry stepRegistry;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public void queueNodeExecution(NodeExecutionProto nodeExecution) {
    nodeExecutionProtoServiceBlockingStub.queueNodeExecution(
        QueueNodeExecutionRequest.newBuilder().setNodeExecution(nodeExecution).build());
  }

  @Override
  public String queueTask(String nodeExecutionId, TaskMode mode, Map<String, String> setupAbstractions, Task task) {
    QueueTaskResponse response = nodeExecutionProtoServiceBlockingStub.queueTask(
        QueueTaskRequest.newBuilder()
            .setTaskMode(mode)
            .putAllSetupAbstractions(setupAbstractions == null ? Collections.emptyMap() : setupAbstractions)
            .setTask(ByteString.copyFrom(kryoSerializer.asBytes(task)))
            .setNodeExecutionId(nodeExecutionId)
            .build());
    return response.getTaskId();
  }

  @Override
  public void addExecutableResponse(
      @NonNull String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    nodeExecutionProtoServiceBlockingStub.addExecutableResponse(AddExecutableResponseRequest.newBuilder()
                                                                    .setNodeExecutionId(nodeExecutionId)
                                                                    .setStatus(status)
                                                                    .setExecutableResponse(executableResponse)
                                                                    .addAllCallbackIds(callbackIds)
                                                                    .build());
  }

  @Override
  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    nodeExecutionProtoServiceBlockingStub.handleStepResponse(HandleStepResponseRequest.newBuilder()
                                                                 .setNodeExecutionId(nodeExecutionId)
                                                                 .setStepResponse(stepResponse)
                                                                 .build());
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return JsonOrchestrationUtils.asObject(stepParameters, step.getStepParametersClass());
  }
}
